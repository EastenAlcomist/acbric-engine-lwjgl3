package com.zarkonnen.airships;

import java.util.LinkedList;
import org.json.JSONObject;
import static com.zarkonnen.airships.Lang._t;
import static com.zarkonnen.airships.LaunchSettings.maxPingUnacknowledgedBytes;
import com.zarkonnen.catengine.util.Utils;
import com.zarkonnen.catengine.util.Utils.Pair;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.JSONArray;

public class Client {
	private final LinkedList<JSONObject> messagesIn = new LinkedList<JSONObject>();
	
	private final LinkedList<Utils.Pair<Long, byte[]>> writeMessageQueue = new LinkedList<Utils.Pair<Long, byte[]>>();
	private final LinkedList<Utils.Pair<Long, byte[]>> unAcknowledgedMessages = new LinkedList<Utils.Pair<Long, byte[]>>();
	
	private Future<Void> connectFuture;
	private AsynchronousSocketChannel socketChannel;
	private ByteBuffer readBuffer = ByteBuffer.allocate(4096);
	private ByteBuffer writeBuffer = ByteBuffer.allocate(4096);
	private int nextMessageSize = -1;
	private Future<Integer> readFuture;
	private Future<Integer> writeFuture;
	private long readFutureStarted;
	private long writeFutureStarted;
	private boolean attemptFullReconnect = false;
	private boolean sendingReconnectMessage = false;
	private boolean awaitingReconnectResponse = false;
	private boolean userRequestsClose = false;
	private boolean sendingBye = false;
	private long byeSent = 0;
	private volatile boolean isClosed = false;
	public final String serverIP;
	private final AirshipGame g;
	private boolean messageTooLarge;
	private final LinkedList<Long> recentPings = new LinkedList<Long>();
	public static final double PING_FALLOFF_MULT = 0.7;
	public static final int RECENT_PING_WINDOW = 5;
	public int tooMuchPingStrikes = 0;
	public long recentPing = 0;
	private long mostRecentPingSentTime = 0;
	private long mostRecentTickTime = 0;
	public long longestTimeBetweenTicksSincePingSent = 0;
	private long pingNonce = 0;
	private boolean waitingForPing;
	private String uniqueID;
	private long messageIDCounter = 1;
	private long mostRecentReceivedMessageID = -1;
	private long reconnectTime;
	private int reconnectAttempt;
	private int unacknowledgedBytes;
	private long crashReconnectTime;
	private long connectTime;
	public int maxConnectAttempts = LaunchSettings.maxConnectAttempts;
	private int connectAttempt = 0;
	private long nextConnectTime = 0;
	private boolean attemptingToConnect = true;
	private boolean prevIterOverload = false;
	private boolean currentIterOverload = false;
	
	private final LinkedList<Integer> tickIntervals = new LinkedList<Integer>();
	
	static AsynchronousChannelGroup group;
	
	static {
		try {
			group = AsynchronousChannelGroup.withFixedThreadPool(2, new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "NIO Thread");
					t.setPriority(Thread.MAX_PRIORITY);
					t.setDaemon(true);
					return t;
				}
			});
		} catch (Exception e) {
			System.out.println("AsynchronousChannelGroup creation failed");
			e.printStackTrace();
		}
	}
	
	public int avgTickInterval() {
		int sum = 0;
		if (tickIntervals.isEmpty()) { return 0; }
		for (int c : tickIntervals) {
			sum += c;
		}
		return sum / tickIntervals.size();
	}
	
	public int maxRecentTickInterval() {
		int max = 0;
		if (tickIntervals.isEmpty()) { return 0; }
		for (int c : tickIntervals) {
			max = Math.max(max, c);
		}
		return max;
	}
	
	public int unacknowledgedBytes() {
		return unacknowledgedBytes;
	}
		
	public long smoothedRecentPing() {
		double total = 0;
		double div = 0;
		double weight = 1;
		long max = 0;
		for (long p : recentPings) {
			max = Math.max(max, p);
		}
		for (long p : recentPings) {
			if (p < max) {
				total += p * weight;
				div += weight;
			}
			weight *= PING_FALLOFF_MULT;
		}
		if (total == 0 || div <= 0) { return 0; }
		return (long) (total / div);
	}
	
	public boolean isMessageTooLarge() {
		return messageTooLarge;
	}
	
	public void clearMessageTooLarge() {
		messageTooLarge = false;
	}
	
	public int outQueueSize() {
		return writeMessageQueue.size();
	}
	
	public int inQueueSize() {
		return messagesIn.size();
	}

	public static JSONObject msg(String type) {
		return new JSONObject().put("type", type).put("t", DateTime.now(DateTimeZone.UTC).getMillis());
	}
	
	public static JSONObject gmsg(String type) {
		return new JSONObject().put("type", type).put("global", true).put("t", DateTime.now(DateTimeZone.UTC).getMillis());
	}

	public JSONObject pollMessageRaw() {
		tick();
		return messagesIn.pollFirst();
	}

	public final boolean sendMessageRawWithSizeCheck(JSONObject msg) {
		byte[] msgData;
		try {
			long mid = messageIDCounter++;
			msg.put("###", mid);
			msgData = msg.toString().getBytes("UTF-8");
			if (msgData.length > LaunchSettings.maxNetworkSendBytes) {
				return false;
			} else {
				writeMessageQueue.add(new Pair<Long, byte[]>(mid, msgData));
				tick();
			}
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace(); // Give up
			return false;
		}
		return true;
	}
	
	public final void sendMessageRaw(JSONObject msg) {
		byte[] msgData;
		try {
			long mid = messageIDCounter++;
			msg.put("###", mid);
			msgData = msg.toString().getBytes("UTF-8");
			if (msgData.length > LaunchSettings.maxNetworkSendBytes) {
				messageTooLarge = true;
			} else {
				writeMessageQueue.add(new Pair<Long, byte[]>(mid, msgData));
				tick();
			}
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace(); // Give up
		}
	}
		
	private void ack(long mid) {
		try {
			writeMessageQueue.add(new Pair<Long, byte[]>(null, new JSONObject().put("type", "ack").put("mid", mid).toString().getBytes("UTF-8")));
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace(); // Give up
		}
	}

	public boolean isConnectedRaw() {
		return !isDisconnected() && (attemptingToConnect || attemptFullReconnect || (socketChannel != null && socketChannel.isOpen()));
	}

	public boolean isDisconnected() {
		return isClosed;
	}

	public boolean isConnecting() {
		return connectFuture != null || attemptingToConnect;
	}
	
	public boolean isReconnecting() {
		return attemptFullReconnect;
	}

	public Client(String playerName, String serverIP, AirshipGame g) {
		System.out.println("new Client");
		this.serverIP = serverIP;
		this.g = g;
		sendMessageRaw(msg("retainMeIWillAckMessages").put("playerName", playerName));
	}
	
	private void reconnectFailure() {
		connectFuture = null;
		if (awaitingReconnectResponse) {
			// We were able to connect, but were not recognized, and rejected.
			attemptFullReconnect = false;
			try {
				socketChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			socketChannel = null;
			isClosed = true;
		} else {
			attemptFullReconnect = true;
			if (reconnectAttempt++ >= LaunchSettings.maxReconnectAttempts) {
				System.err.println("Reconnect failure. Giving up." + "@ " + System.currentTimeMillis() + " " + new DateTime());
				attemptFullReconnect = false;
				try {
					socketChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				socketChannel = null;
				isClosed = true;
				attemptingToConnect = false;
			} else {
				System.err.println("Reconnect failure. Waiting." + "@ " + System.currentTimeMillis() + " " + new DateTime());
				reconnectTime = System.currentTimeMillis() + LaunchSettings.reconnectAttemptIntervalMilliseconds;
			}
		}
	}
	
	private void connectFailureClose() {
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		connectFuture = null;
		socketChannel = null;
		if (connectAttempt++ >= maxConnectAttempts) {
			System.out.println("giving up on connecting");
			isClosed = true;
			attemptingToConnect = false;
		} else {
			System.out.println("attempting to connect again in the future " + connectAttempt);
			nextConnectTime = System.currentTimeMillis() + LaunchSettings.connectAttemptIntervalMilliseconds;
			attemptingToConnect = true;
		}
	}
		
	public void tick() {
		/*if (AGame.ANIM_R.nextInt(1000) == 33) { // qqDPS Occasional spanner-wrenching.
			System.out.println("wrench!");
			try { Thread.sleep(5000); } catch (Exception e) {}
		}*/
		
		final long currentTime = System.currentTimeMillis();
		crashReconnectTime = Math.max(crashReconnectTime, currentTime);
		if (mostRecentTickTime == 0) {
			mostRecentTickTime = currentTime;
		} else {
			tickIntervals.add((int) (currentTime - mostRecentTickTime));
			if (tickIntervals.size() > 1000) {
				tickIntervals.remove(0);
			}
		}
		longestTimeBetweenTicksSincePingSent = Math.max(currentTime - mostRecentTickTime, longestTimeBetweenTicksSincePingSent);
		mostRecentTickTime = currentTime;
		prevIterOverload = currentIterOverload;
		currentIterOverload = false;

		/*if (isClosed) {
			System.out.println("isClosed; isConnected = " + isConnectedRaw());
		}*/
		
		for (int iteration = 0; iteration < 32; iteration++) {
			if (isClosed) { return; }
			
			if (iteration > 10) {
				/*if (!currentIterOverload) {
					System.out.println("iter overload");
				}*/
				currentIterOverload = true;
			}

			if (userRequestsClose && (byeSent != 0 || attemptFullReconnect)) {
				if (attemptFullReconnect || currentTime > byeSent + 5000) {
					try {
						socketChannel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					isClosed = true;
					System.err.println("Closed on user behalf. afr=" + attemptFullReconnect + "@ " + currentTime + " " + new DateTime());
				}/* else {
					System.err.println("Waiting for bye sent timeout. currentTime " + currentTime + " byeSent " + byeSent + " userRequestClose " + userRequestsClose);
				}*/
				return;
			}
						
			if (attemptFullReconnect) {
				if (currentTime < reconnectTime || currentTime < crashReconnectTime - LaunchSettings.crashReconnectBackoffMilliseconds) {
					return;
				}
				if (connectFuture == null) {
					System.err.println("Resetting for reconnect @ " + currentTime);
					// Uh, oh god. Right.
					waitingForPing = false;
					readFuture = null;
					writeFuture = null;
					connectFuture = null;
					readBuffer = ByteBuffer.allocate(4096);
					writeBuffer = ByteBuffer.allocate(4096);
					nextMessageSize = -1;
					writeMessageQueue.addAll(0, unAcknowledgedMessages);
					unAcknowledgedMessages.clear();
					unacknowledgedBytes = 0;
					pingNonce = 0; // Any pings we tried to send don't count.
					tooMuchPingStrikes = 0;
					awaitingReconnectResponse = false;
					sendingReconnectMessage = false;
					recentPings.clear(); // Ping history is now meaningless.
					
					try {
						socketChannel.close();
					} catch (Exception e) {}
					
					try {
						socketChannel = group == null ? AsynchronousSocketChannel.open() : AsynchronousSocketChannel.open(group);
						socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
						socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
						connectFuture = socketChannel.connect(new InetSocketAddress(serverIP, Server.RECONNECT_PORT));
						System.out.println("reconnecting");
						connectTime = currentTime;
						System.err.println("Created connect future" + "@ " + currentTime + " " + new DateTime());
					} catch (Exception e) {
						e.printStackTrace();
						reconnectFailure();
						return;
					}
				} else {
					if (connectFuture.isDone()) {
						try {
							connectFuture.get();
						} catch (Exception e) {
							e.printStackTrace();
							reconnectFailure();
							return;
						}
						connectFuture = null;
						if (!socketChannel.isOpen()) {
							reconnectFailure();
							return;
						}
						try {
							writeMessageQueue.add(0, new Pair<Long, byte[]>(null, msg("fullReconnect").put("uniqueID", uniqueID).toString().getBytes("UTF-8")));
							sendingReconnectMessage = true;
							System.err.println("Sent fullReconnect message" + "@ " + currentTime + " " + new DateTime());
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						attemptFullReconnect = false;
						reconnectTime = currentTime;
					} else if (connectTime + LaunchSettings.reconnectTimeout < currentTime) {
						reconnectFailure();
						return;
					}
				}
				
				return;
			}
			
			if (socketChannel == null) {
				if (nextConnectTime > currentTime) { return; }
				try {
					socketChannel = group == null ? AsynchronousSocketChannel.open() : AsynchronousSocketChannel.open(group);
					socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
					socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
					connectFuture = socketChannel.connect(new InetSocketAddress(serverIP, Server.PORT));
					connectTime = currentTime;
					System.out.println("connecting");
				} catch (Exception e) {
					System.err.println("socketChannel failureClose" + "@ " + currentTime + " " + new DateTime());
					e.printStackTrace();
					connectFailureClose();
					return;
				}
			}

			if (connectFuture != null) {
				if (connectFuture.isDone()) {
					try {
						connectFuture.get();
					} catch (Exception e) {
						connectFailureClose();
						return;
					}
					connectFuture = null;
					if (!socketChannel.isOpen()) {
						connectFailureClose();
						return;
					}
					attemptingToConnect = false;
				} else if (connectTime + LaunchSettings.connectTimeout < currentTime) {
					connectFailureClose();
					return;
				} else {
					return;
				}
			}
			
			if (
					!userRequestsClose &&
					(writeFuture == null || writeFuture.isDone()) &&
					unacknowledgedBytes < maxPingUnacknowledgedBytes &&
					!isConnecting() &&
					!waitingForPing &&
					mostRecentPingSentTime + LaunchSettings.measureNetworkDelayEveryMilliseconds < currentTime &&
					reconnectTime + LaunchSettings.minimumLagReconnectInterval < currentTime &&
					!currentIterOverload &&
					!prevIterOverload)
			{
				pingNonce = AGame.ANIM_R.nextLong();
				try {
					// Sending this directly because sendRaw would cause a stack overflow.
					writeMessageQueue.add(0, new Pair<Long, byte[]>(null, msg("ping").put("timestamp", currentTime).put("nonce", pingNonce).toString().getBytes("UTF-8")));
					//System.out.println("sending ping " + pingNonce);
					//System.out.println("sending ping with wmq l " + writeMessageQueue.size() + " nonce " + pingNonce);
					waitingForPing = true;
					mostRecentPingSentTime = currentTime;
					longestTimeBetweenTicksSincePingSent = 0;
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

			if (writeFuture != null) {
				if (writeFuture.isDone()) {
					try {
						writeFuture.get();
						/*
						if (start + 10000 < System.currentTimeMillis()) {
							System.out.println("WRENCHHHHH");
							start = System.currentTimeMillis();
							throw new RuntimeException("Wrench");
						}
						*/
					} catch (Exception e) {
						if (userRequestsClose) {
							System.err.println("writeFuture exception" + "@ " + currentTime + " " + new DateTime());
							e.printStackTrace();
							System.err.println("Bye sent, effectively." + "@ " + currentTime + " " + new DateTime());
							byeSent = currentTime;
						} else {
							System.err.println("writeFuture exception" + "@ " + currentTime + " " + new DateTime());
							e.printStackTrace();
							System.err.println("Attempt full reconnect, please (writeFuture.get crash) @ " + currentTime + " " + new DateTime());
							attemptFullReconnect = true;
							crashReconnectTime += LaunchSettings.crashReconnectBackoffMilliseconds;
						}
						return;
					}
					writeFuture = null;
					if (sendingBye) {
						System.err.println("Bye sent." + "@ " + currentTime + " " + new DateTime());
						byeSent = currentTime;
						return;
					}
					if (sendingReconnectMessage) {
						System.err.println("Reconnect message sent." + "@ " + currentTime + " " + new DateTime());
						sendingReconnectMessage = false;
						awaitingReconnectResponse = true;
					}
					if (writeBuffer.hasRemaining()) {
						//System.out.println("more write needed");
						writeFuture = socketChannel.write(writeBuffer);
						writeFutureStarted = currentTime;
					}
				} else if (writeFutureStarted + LaunchSettings.tooMuchNetworkDelayMilliseconds < currentTime) {
					System.err.println("Write timeout.");
					System.err.println("Attempt full reconnect, please (write timeout) @ " + currentTime + " " + new DateTime());
					attemptFullReconnect = true;
					crashReconnectTime += LaunchSettings.crashReconnectBackoffMilliseconds;
					return;
				}
			}
			if (writeFuture == null && !writeMessageQueue.isEmpty() && unacknowledgedBytes < LaunchSettings.maxNetworkSendBytes) {
				try {
					//System.out.println("-> " + msgString);
					Pair<Long, byte[]> idAndMsg = writeMessageQueue.pollFirst();
					byte[] msg = idAndMsg.b;
					if (idAndMsg.a != null) {
						unAcknowledgedMessages.add(idAndMsg);
						unacknowledgedBytes += msg.length;
					}
					if (writeBuffer.capacity() < msg.length + 4) {
						writeBuffer = ByteBuffer.allocate(msg.length + 4 + 128);
					}
					writeBuffer.clear();
					writeBuffer.putInt(msg.length);
					writeBuffer.put(msg);
					writeBuffer.flip();
					writeFuture = socketChannel.write(writeBuffer);
					writeFutureStarted = currentTime;
					if (userRequestsClose) {
						System.err.println("Sending bye." + "@ " + currentTime + " " + new DateTime());
						sendingBye = true;
					}
				} catch (NotYetConnectedException nyce) {
					if (userRequestsClose) {
						System.err.println("writeFuture nyce" + "@ " + currentTime + " " + new DateTime());
						byeSent = currentTime;
					} else {
						System.err.println("NotYetConnectedException when writing");
						System.err.println("Attempt full reconnect, please (write NYCE) @ " + currentTime + " " + new DateTime());
						attemptFullReconnect = true;
						crashReconnectTime += LaunchSettings.crashReconnectBackoffMilliseconds;
						if (awaitingReconnectResponse) {
							reconnectFailure();
						}
					}
					return;
				}
			}

			if (readFuture == null) {
				try {
					readFuture = socketChannel.read(readBuffer);
					readFutureStarted = currentTime;
				} catch (NotYetConnectedException nyce) {
					if (userRequestsClose) {
						System.err.println("readFuture nyce" + "@ " + currentTime + " " + new DateTime());
						byeSent = currentTime;
					} else {
						System.err.println("NotYetConnectedException when reading");
						System.err.println("Attempt full reconnect, please (read NYCE) @ " + currentTime + " " + new DateTime());
						attemptFullReconnect = true;
						crashReconnectTime += LaunchSettings.crashReconnectBackoffMilliseconds;
						if (awaitingReconnectResponse) {
							reconnectFailure();
						}
					}
					return;
				}
			}
			int readFutureResult;
			try {
				readFutureResult = readFuture.get(1, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				readFutureResult = -2;
				iteration = 100; // Nothing in the pipe, don't try again this tick.
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Attempt full reconnect, please (readfuture.get crash) @ " + currentTime + " " + new DateTime());
				if (awaitingReconnectResponse) {
					reconnectFailure();
				}
				attemptFullReconnect = true;
				crashReconnectTime += LaunchSettings.crashReconnectBackoffMilliseconds;
				return;
			}
			if (readFutureResult != -2) {
				if (readFutureResult == -1) {
					attemptFullReconnect = true;
					System.err.println("-1 when reading");
					System.err.println("Attempt full reconnect, please (readFuture.get) @ " + currentTime + " " + new DateTime());
					crashReconnectTime += LaunchSettings.crashReconnectBackoffMilliseconds;
					if (awaitingReconnectResponse) {
						reconnectFailure();
					}
					return;
				}
				awaitingReconnectResponse = false;
				reconnectAttempt = 0; // We have fully reconnected.
				readBuffer.flip();
				boolean progress = true;
				while (progress) {
					progress = false;
					if (nextMessageSize == -1 && readBuffer.remaining() >= 4) {
						progress = true;
						nextMessageSize = readBuffer.getInt();
						if (nextMessageSize > readBuffer.capacity()) {
							ByteBuffer rb2 = ByteBuffer.allocate(nextMessageSize + 1024);
							rb2.put(readBuffer);
							readBuffer = rb2;
							readBuffer.flip();
						}
					}
					if (nextMessageSize != -1 && readBuffer.remaining() >= nextMessageSize) {
						progress = true;
						byte[] msgBytes = new byte[nextMessageSize];
						readBuffer.get(msgBytes);
						try {
							JSONObject inMsg = new JSONObject(new String(msgBytes, "UTF-8"));
							boolean skip = false;
							if (inMsg.has("###")) {
								long mid = inMsg.getLong("###");
								skip = mostRecentReceivedMessageID >= mid;
								if (!skip) { mostRecentReceivedMessageID = mid; }
								ack(mid);
							}
							if (skip) {
								System.err.println("Skip" + "@" + currentTime);
								reconnectTime = currentTime;
							}
							if (!skip) {
								messagesIn.add(inMsg);
								if (inMsg.optString("type", "?").equals("ack")) {
									long mid = inMsg.getLong("mid");
									for (int j = 0; j < unAcknowledgedMessages.size(); j++) {
										if (unAcknowledgedMessages.get(j).a <= mid) {
											unacknowledgedBytes -= unAcknowledgedMessages.get(j).b.length;
											unAcknowledgedMessages.remove(j);
										}
									}
								}
								if (inMsg.optString("type", "?").equals("welcome")) {
									// The ping response may be sent to our old channel, so we would not be in a state to receive it.
									waitingForPing = false;
									pingNonce = 0;
									//System.out.println("welcome: clearing old ping"); // qqDPS
								}
								if (inMsg.optString("type", "?").equals("assignID")) {
									uniqueID = inMsg.optString("uniqueID", null);
								}
								if (waitingForPing && inMsg.optString("type", "?").equals("frame")) {
									JSONArray a = inMsg.getJSONArray("messages");
									for (int j = 0; j < a.length(); j++) {
										JSONObject frameMsg = a.getJSONObject(j);
										if (frameMsg.optString("type", "?").equals("ping") && frameMsg.getLong("nonce") == pingNonce) {
											//System.out.println("received ping " + pingNonce);
											recentPing = currentTime - frameMsg.getLong("timestamp") - longestTimeBetweenTicksSincePingSent;
											recentPings.add(0, recentPing);
											if (recentPings.size() > RECENT_PING_WINDOW) {
												recentPings.remove(recentPings.size() - 1);
											}
											waitingForPing = false;
											if (recentPing > LaunchSettings.tooMuchNetworkDelayMilliseconds && reconnectTime + LaunchSettings.minimumLagReconnectInterval < currentTime) {
												tooMuchPingStrikes++;
												if (tooMuchPingStrikes >= LaunchSettings.tooMuchNetworkDelayStrikes) {
													attemptFullReconnect = true;
													System.out.println("Ping Reconnect");
													System.err.println("Attempt full reconnect, please (ping) @ " + currentTime + " " + new DateTime());
												}
												System.err.println("Bad ping " + recentPing + " with " + unacknowledgedBytes + " unacknowledged bytes");
											} else {
												tooMuchPingStrikes = 0;
											}
											break;
										}
									}
								}
							}
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
							// Give up on life.
						}
						nextMessageSize = -1;
						if (readBuffer.remaining() >= 4) {
							nextMessageSize = readBuffer.getInt();
							if (nextMessageSize > readBuffer.capacity()) {
								ByteBuffer rb2 = ByteBuffer.allocate(nextMessageSize + 1024);
								rb2.put(readBuffer);
								readBuffer = rb2;
								readBuffer.flip();
							}
						}
					}
				}
				readBuffer.compact();
				if (nextMessageSize != -1) {
					readBuffer.limit(nextMessageSize);
				} else {
					readBuffer.limit(4);
				}
				readFuture = socketChannel.read(readBuffer);
				readFutureStarted = currentTime;
			} else if (readFutureStarted + LaunchSettings.tooMuchNetworkDelayMilliseconds < currentTime && reconnectTime + LaunchSettings.tooMuchNetworkDelayMillisecondsOnReconnect < currentTime) {
				System.err.println("Read timeout @ " + currentTime + " " + new DateTime());
				attemptFullReconnect = true;
				crashReconnectTime += LaunchSettings.crashReconnectBackoffMilliseconds;
				if (awaitingReconnectResponse) {
					reconnectFailure();
				}
				return;
			}
		}
		
		mostRecentTickTime = System.currentTimeMillis();
	}
	
	public void close() {
		if (userRequestsClose) { return; }
		try {
			writeMessageQueue.add(0, new Pair<Long, byte[]>(null, msg("bye").toString().getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			// ???
		}
		System.err.println("Bye enqueued." + " @ " + System.currentTimeMillis() + " " + new DateTime());
		userRequestsClose = true;
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!isClosed) { tick(); }
				System.err.println("Client closed." + " @ " + System.currentTimeMillis() + " " + new DateTime());
				AirshipGame.instance.delayExitForNetwork = 0;
			}
		});
		t.setDaemon(true);
		t.setName("Client closer thread");
		t.start();
		AirshipGame.instance.delayExitForNetwork = 6000;
	}
}
