package com.zarkonnen.airships;

import com.zarkonnen.catengine.Fount;
import com.zarkonnen.catengine.util.Clr;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.FileUtils;

public final strictfp class  AGame {
	private AGame() {}

	public static final String ALPHABET = " qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890-=+_!?<>,.;:\"'@£$%^&*()[]{}|\\~/±ієїґІЄЇҐåäöüÄÖÜÉÊÈéêèëÇçÅÀàâûąćęłńóśżźĄĆĘŁŃÓŚŻŹАаБбВвГгДдЕеЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЬьЮюЯяЫыЭэЪъЁё`¡¿ÁáíñúđďþňčřžšßťǎăāæěĕēòôøœůÿïĐĎÞÑŇČŘŽŠSSŤÂǍĂĀÆËÂĚĔĒÒÔØŒŮŸÏÍ”…　、。々あいうえおかがきぎくぐけげこごさしじすずせぜそぞただちっつづてでとどなにぬねのはばひびぶへべほぼぽまみむめもゃやゅゆょよらりるれろわをんァアィイウェエォオカガキギクグケゲコゴサザシジスズセゼソゾタダチッツテデトドナニネノハバパヒビピフブプヘベペホボポマミムメモャヤュユョラリルレロワンヴ・ー一万丈三上下不与世両並中丸主久乗乱乾了予争事二互亡交人今介仕他付代令以仲件任伏伝伴伸似位低住体何作使供依価侵係保信修倉個倍倒値停側偵備催債傷働像儀償優元充先光党入全公共兵具内円再写冠冶処凹出刃分切刑初判別利到制刻削前剖副創力功加劣助努労効勇動務勝勢包化北匹区医匿十半卒協南単占印危即卵厚厳去参及反収取受口古句叩可台右司各合同名向君否含吸吹告周味呼命和品員哨哲唆唱唾商問善喜喰営噂器四回団困囲図固国土圧在地垂型城域基報場塔塗塞塩填境増墜壁壊壌士壮売変夕外多夜大太夫央失奇契奨奪奴好如妙妨始威子字存学孵守安完宗官定宝実客宣室害家容宿寄密富寝察寸寺対専射将尋導小少尖尽尾局居屈屋展属層屯山岩峙島崇崩嵐巣工左巧巨市帆希帝帯帰常幅干平年幸幹広底店度座庫延建式引弱張強弾当形彫影役彼征径待後徐従得御復徹心必忘応快念怖思急性怪恐悟悪情惨意感態慎憶懇懸成我戒戦戻所扇扉手打払扱技抑投抗折抜択抵押抽担拒拠拡拿持指挑振挺捉捕捜捨捻授排掘掛採探接推掲掴揃描提揚換揮援揺損搬搭搾撃撤操支改攻放政故救敗教敢散数整敵敷文料斜断新方施旅旋旗既日旧早昆昇明易星昼時晴晶暮暴曲更書替最有服望期木未末本材村杖条来東板析林果枝枠枯柔査栄根格案梯械棄棚森検業極楽構様槽標模権樫樽機欠次欲歓止正武歩死残殴段殺殻殿毎毒比民気水永求池決治況泊法注洋洗活派流浅浪浮海消深渇済減測満源準溶滅滴漏演漬潜火灰災炉炎炭炸点為烈無焦然焼煉煙照煽熟熱燃燥爆片牛物牲特犠犯状狂狙狩独猛献獲率王珍現球理璧環瓦生産用田由甲申画界留畜略番異疑療発登白的皆皮皿益盗盟監盤目直相真眠着瞬矢知短石研砕砲破硬確磨礎示祈神禁秀私科秒秘移程税種稼積穏穴究空突窓窮立竜章端笏笑笛第筒策箔算管節範築簡粉粒粗精約紅紋納純級素索細紹終組絆経結絞給統絶継続維網綻緑線編緩練縦縮績繁繋織繰罪置羅美群義翻翼考者耐肉肢育胆背能脅脆脚脱腐腹膠臨自臭至致舎航舷船艇艘艦良色芸苦茂草荒荷莫落葉著蒸蓄蔵薄薬蘇虐虫融血行術街衛衝衡表被裁裂装裏補製複襲西要覇見規視覚覧観角解触言計訓記訣訪設許訳証詠試詰話詳誇認語説読誰調諜謝識警議護豊象負財貢貨販貪貫貯買費貼賂賄資賊質購贈赤走起超足距跡路跳身車軌軍軟転軽較載輝輪輸辺込辿迎近返迫追退送逃逆透逐途通速造連週進遅遇遊運過道達遙遠遣適遭選避邪部都配酬酷酸醸重野量金鉄鉱銀銃銅鋭鋼錆錬録鍮鎖鎮鏡長門閉開間関闇闘防阻降限院除陥陸険陽隊階際障隠隣集雇離難雨雪雷電震霧露青非面革鞄音響頃項順頑頓領頭頻頼題願類飛食飾養餌首駄駆駐騎騒験驚骨骸高魔魚黄黒！％＆（）＋－０１２３４５６７８：？ｓ～ｽﾃﾅﾒﾝ—“”…、。《》㤘㧐㧟㸆䁖䏝䥽䦃一丁七万丈三上下不与丏丐丑专且丕世丘丙业丛东丝丞丢两严丧个丫中丰串临丸丹为主丽举乂乃久么义之乌乍乎乏乐乒乓乔乖乘乙乜九乞也习乡书乩买乱乳乾了予争事二于亏云互亓五井亘亚些亟亡亢交亥亦产亨亩享京亭亮亲亳亵亶人亿什仁仂仃仄仅仆仇仉今介仍从仑仓仔仕他仗付仙仞仟仡代令以仨仪仫们仰仲仵件价任份仿企伉伊伍伎伏伐休众优伙会伛伞伟传伢伤伥伦伧伪伫伯估伴伶伸伺似伽佃但位低住佐佑体何佗佘余佚佛作佝佞佟你佣佤佥佩佬佯佰佳佶佻佼佾使侃侄侈侉例侍侏侑侔侗供依侠侣侥侦侧侨侩侪侬侮侯侵便促俄俅俊俎俏俐俑俗俘俚保俞俟信俣俦俨俩俪俭修俯俱俳俵俶俸俺俾倌倍倏倒倔倘候倚倜借倡倥倦倨倩倪倬倭债值倾偃假偈偌偎偏偕做停健偬偶偷偻偾偿傀傅傈傍傣傥傧储傩催傲傻像僖僚僧僭僮僰僳僵僻儆儋儒儡儿兀允元兄充兆先光克免兑兔兕兖党兜兢入全八公六兮兰共关兴兵其具典兹养兼兽冀内冈冉册再冒冕冗写军农冠冢冤冥冬冯冰冲决况冶冷冻冼冽净凄准凇凉凋凌减凑凛凝几凡凤凫凭凯凰凳凶凸凹出击凼函凿刀刁刃分切刈刊刍刎刑划刖列刘则刚创初删判刨利别刭刮到刳制刷券刹刺刻刽刿剀剁剂剃削剋剌前剐剑剔剖剜剞剡剥剧剩剪副割剽剿劁劈劓力劝办功加务劢劣动助努劫劬劭励劲劳劼劾势勃勇勉勋勐勒勖勘募勤勰勺勾勿匀包匆匈匍匏匐匕化北匙匜匝匠匡匣匦匪匮匹区医匾匿十千卅升午卉半华协卑卒卓单卖南博卜卞卟占卡卢卣卤卦卧卫卮卯印危即却卵卷卸卿厂厄厅历厉压厌厍厕厘厚厝原厢厥厦厨厩厮去厾县叁参叆又叉及友双反发叔取受变叙叛叟叠口古句另叨叩只叫召叭叮可台叱史右叵叶号司叹叻叼叽吁吃各吆合吉吊同名后吏吐向吓吕吗君吝吞吟吠吡吣否吧吨吩含听吭吮启吱吲吴吵吸吹吻吼吽吾呀呃呆呈告呋呐呒呓呔呕呖呗员呛呜呢呣呤呦周呱呲味呵呶呷呸呻呼命咀咂咄咆咋和咎咏咐咒咔咕咖咙咚咛咝咣咤咦咧咨咩咪咫咬咯咱咳咴咸咻咽咿哀品哂哄哆哇哈哉哌响哎哏哐哑哓哔哕哗哙哚哝哞哟哥哦哧哨哩哪哭哮哲哺哼哽唁唆唇唉唏唐唑唔唛唠唢唣唤唧唬售唯唰唱唳唵唷唼唾唿啁啃啄商啉啊啐啕啖啜啡啤啥啦啧啪啬啭啮啰啵啶啷啸啻啼啾喀喁喂喃善喇喈喉喊喋喏喑喔喘喙喜喝喟喧喱喳喵喷喹喻喽喾嗄嗅嗉嗌嗍嗐嗑嗒嗓嗔嗖嗜嗝嗞嗟嗡嗣嗤嗥嗦嗨嗪嗫嗬嗯嗲嗳嗵嗷嗽嗾嘀嘁嘈嘉嘌嘎嘘嘚嘛嘞嘟嘡嘣嘤嘧嘬嘭嘱嘲嘴嘶嘹嘻嘿噌噍噎噔噗噘噙噜噢噤器噩噪噫噬噱噶噻噼嚄嚅嚆嚎嚏嚓嚣嚯嚷嚼囊囔囚四回囟因囡团囤囫园困囱围囵囹固国图囿圃圄圆圈圉圊圜土圣在圩圪圬圭圮圯地圳圹场圻圾址坂均坊坌坍坎坏坐坑块坚坛坝坞坟坠坡坤坦坨坩坪坫坭坯坳坷坻坼垂垃垄垆型垌垒垓垛垠垡垢垣垤垦垧垩垫垭垮垱垸埂埃埋城埒埔埕埘埙埚埝域埠埤埭埯埴埵埸培基埽堂堆堇堉堋堍堑堕堙堞堡堤堪堰堵塄塆塌塍塑塔塘塞填塬塾墀墁境墅墉墒墓墙增墟墨墩墼壁壅壑壕壤士壬壮声壳壶壹处备复夏夔夕外夙多夜够夤大天太夫夬夭央夯失头夷夸夹夺夼奁奂奄奇奈奉奋奎奏契奔奕奖套奘奚奠奢奥奭女奴奶奸她好妁如妃妄妆妇妈妊妍妒妓妖妗妙妞妣妤妥妨妩妪妫妮妯妲妹妻妾姆姊始姐姑姒姓委姗姘姚姜姝姣姥姨姬姮姹姻姿威娃娄娅娆娇娈娉娌娑娓娘娜娟娠娣娥娩娱娲娴娶娼婀婆婉婊婕婚婢婧婪婴婵婶婷婺婿媒媚媛媪媭媲媳媵媸媾嫁嫂嫉嫌嫒嫔嫖嫘嫚嫠嫡嫣嫦嫩嫫嫱嬉嬖嬗嬴嬷孀子孑孓孔孕字存孙孚孛孜孝孟孢季孤孥学孩孪孬孰孱孳孵孺孽宁它宅宇守安宋完宏宓宕宗官宙定宛宜宝实宠审客宣室宥宦宪宫宰害宴宵家宸容宽宾宿寂寄寅密寇富寐寒寓寝寞察寡寤寥寨寮寰寸对寺寻导寿封射将尉尊小少尔尕尖尘尚尜尝尤尥尧尬就尴尸尹尺尻尼尽尾尿局屁层居屈屉届屋屎屏屐屑展屙属屠屡屣履屦屯山屹屺屿岁岂岈岌岐岑岔岖岗岘岚岛岢岣岩岫岬岭岱岳岷岸岿峁峄峋峒峙峡峣峤峥峦峨峪峭峰峻崂崃崆崇崎崔崖崚崛崤崦崩崭崮崴崽嵇嵊嵋嵌嵎嵖嵘嵛嵝嵩嵫嵬嵯嵴嶂嶙嶝嶷巅巉巍川州巡巢工左巧巨巩巫差巯己已巳巴巷巽巾币市布帅帆师希帏帐帑帔帕帖帘帙帚帛帜帝带帧席帮帷常帻帼帽幂幄幅幌幔幕幛幞幡幢干平年并幸幺幻幼幽广庄庆庇床庋序庐庑库应底庖店庙庚府庞废庠庥度座庭庵庶康庸庹庾廉廊廓廖廛廨廪延廷建廿开弁异弃弄弈弊弋式弑弓引弗弘弛弟张弥弦弧弩弭弯弱弹强弼彀归当录彗彘彝形彤彦彧彩彪彬彭彰影彷役彻彼往征徂径待徇很徉徊律後徐徒徕得徘徙徜御徨循徭微徵德徼徽心必忆忌忍忏忐忑忒忖志忘忙忝忠忡忤忧忪快忭忱念忸忻忽忾忿怀态怂怃怄怅怆怍怎怏怒怔怕怖怙怛怜思怠怡急怦性怨怩怪怫怯怵总怼怿恁恂恃恋恍恐恒恓恕恙恚恢恣恤恨恩恪恫恬恭息恰恳恶恸恹恺恻恼恽恿悄悉悌悍悒悔悖悚悛悝悟悠患悦您悫悬悭悯悱悲悴悸悻悼情惆惇惊惋惑惕惘惚惜惝惟惠惦惧惨惩惫惬惭惮惯惰想惴惶惹惺愀愁愆愈愉愎意愔愕愚感愠愣愤愦愧愫愿慈慊慌慎慑慕慝慢慧慨慰慵慷憋憎憔憧憨憩憬憾懂懈懊懋懑懒懦懵懿戆戈戊戌戍戎戏成我戒戕或戗战戚戛戟戡戢戥截戬戮戳戴户戽戾房所扁扃扇扈扉手才扎扑扒打扔托扛扣扦执扩扪扫扬扭扮扯扰扳扶批扼找承技抃抄抉把抑抒抓抔投抖抗折抚抛抟抠抡抢护报抨披抬抱抵抹抻押抽抿拂拃拄担拆拇拈拉拊拌拍拎拐拒拓拔拖拗拘拙拚招拜拟拢拣拤拥拦拧拨择括拭拮拯拱拳拴拷拼拽拾拿持挂指挈按挎挑挖挚挛挝挞挟挠挡挣挤挥挦挨挪挫振挲挹挺挽捂捃捅捆捉捋捌捍捎捏捐捕捞损捡换捣捧捩捭据捯捶捷捺捻捽掀掂掇授掉掊掌掏掐排掖掘掠探掣接控推掩措掬掮掰掳掴掷掸掺掼掾揄揆揉揍描提插揖揠握揣揩揪揭揳援揶揸揽揿搀搁搂搅搋搏搐搓搔搛搜搞搠搡搦搪搬搭搴携搽摁摄摅摆摇摈摊摒摔摘摞摧摩摭摸摹摽撂撄撅撇撑撒撕撙撞撤撩撬播撮撰撵撷撸撺撼擀擂擅操擎擒擘擞擢擤擦攀攉攒攘攥攫攮支收攸改攻放政故效敌敏救敕敖教敛敝敞敢散敦敫敬数敲整敷文斋斌斐斑斓斗料斛斜斟斡斤斥斧斩斫断斯新方於施旁旃旄旅旆旋旌旎族旒旖旗无既日旦旧旨早旬旭旮旯旰旱时旷旸旺旻昀昂昃昆昉昊昌明昏易昔昕昙昝星映春昧昨昭是昱昴昵昶昼显晁晃晋晌晏晒晓晕晗晚晞晟晡晤晦晨普景晰晴晶晷智晾暂暄暇暌暑暖暗暝暧暨暮暴暹暾曙曛曜曝曦曩曰曲曳更曷曹曼曾替最月有朋服朐朔朕朗望朝期朦木未末本札术朱朴朵机朽杀杂权杆杈杉杌李杏材村杓杖杜杞束杠条来杨杪杭杯杰杲杳杵杷杻杼松板极构枇枉枋析枕林枚果枝枞枢枣枥枧枨枪枫枭枯枰枳枵架枷枸柁柄柈柏某柑柒染柔柘柙柚柜柝柞柠柢查柩柬柯柰柱柳柴柽柿栀栅标栈栉栊栋栌栎栏树栓栖栗栝栟校栩株栲栳样核根格栽栾桀桁桂桃桅框案桉桌桎桐桑桓桔桕桡桢档桤桥桦桧桨桩桫桴桶桷梁梃梅梆梏梓梗梢梦梧梨梭梯械梳梵梿检棁棂棉棋棍棒棕棘棚棠棣森棰棱棵棹棺棻棼椁椅椋植椎椐椒椟椠椤椪椭椰椴椽椿楂楔楚楝楠楣楦楫楮楯楷楸楹楼概榄榆榈榉榔榕榖榛榜榧榨榫榭榱榴榷榻槁槊槌槎槐槔槛槟槠槭槲槽槿樊樗樘樟模樨横樯樱樵樽樾橄橇橐橘橙橛橡橱橹橼檀檄檎檐檗檠檩檬欠次欢欣欤欧欲欸欺款歃歆歇歉歌歙止正此步武歧歪歹死歼殁殂殃殄殆殇殉殊残殍殒殓殖殚殛殡殪殴段殷殿毁毂毅毋母每毒毓比毕毖毗毙毛毡毪毫毯毳毹毽氅氆氇氍氏氐民氓气氖氘氙氚氛氟氡氢氤氦氧氨氩氪氮氯氰氲水永汀汁求汆汇汉汊汐汔汕汗汛汜汝汞江池污汤汨汩汪汰汲汴汶汹汽汾沁沂沃沅沆沈沉沌沏沐沓沔沙沚沛沟没沣沤沥沦沧沨沩沪沫沭沮沱河沸油治沼沽沾沿泄泅泉泊泌泐泓泔法泖泗泛泞泠泡波泣泥注泪泫泮泯泰泱泳泵泷泸泺泻泼泽泾洁洄洇洋洌洎洒洗洙洛洞津洧洪洫洮洱洲洳洵洹活洼洽派流浃浅浆浇浊测浍济浏浑浒浓浔浕浙浚浜浞浠浣浥浦浩浪浮浯浴海浸涂涅消涉涌涎涑涓涔涕涛涝涞涟涠涡涣涤润涧涨涩涪涫涮涯液涵涸涿淀淄淅淆淇淋淌淑淖淘淙淝淞淠淡淤淦淫淬淮深淳混淹添清渊渌渍渎渐渑渔渗渚渝渠渡渣渤渥温渫渭港渲渴游渺湃湄湉湍湎湔湖湘湛湜湟湫湮湲湾湿溃溅溆溉溏源溘溜溟溢溥溧溪溯溱溲溴溶溷溺溻溽滁滂滃滇滋滏滑滓滔滕滗滚滞滟满滢滤滥滦滨滩滪滫滴滹漂漆漉漏漓演漕漠漩漪漫漭漯漱漳漶漾潇潋潍潘潜潞潟潢潦潭潮潲潴潸潺潼澄澈澉澌澍澎澜澡澥澧澳澶澹激濂濉濑濒濞濠濡濮濯瀑瀚瀛瀣灌灏灞火灭灯灰灵灶灸灼灾灿炀炅炉炊炎炒炔炕炖炘炙炜炝炫炬炭炮炯炱炳炷炸点炻炼炽烀烁烂烃烈烊烘烙烛烜烟烤烦烧烨烩烫烬热烯烷烹烽焉焊焐焓焕焖焗焘焙焚焜焦焯焰焱然煅煊煌煎煜煞煤煦照煨煮煲煳煸煺煽熄熊熏熔熘熙熜熟熠熥熨熬熵熹燃燎燔燕燠燥燧燮燹爆爝爨爪爬爰爱爵父爷爸爹爻爽爿牁牂片版牌牍牒牖牙牛牝牟牡牢牤牦牧物牮牯牲牵特牺牾犀犁犄犊犋犍犏犒犟犬犯犰状犷犸犹狁狂狄狈狍狎狐狒狗狙狞狠狡狨狩独狭狮狯狰狱狲狳狷狸狺狼猁猃猎猕猖猗猛猜猝猞猡猢猥猩猪猫猬献猱猴猷猹猾猿獐獒獗獠獬獭獴獾玄率玉王玎玑玕玖玙玛玠玡玢玥玦玩玫玭玮环现玲玳玷玺玻珀珂珈珉珊珍珏珐珑珙珞珠珣珥珧珩班珰珲球琅理琇琉琏琐琚琛琢琤琥琦琨琪琫琬琮琯琰琳琴琵琶琼瑁瑄瑕瑗瑙瑚瑛瑜瑞瑟瑭瑰瑶瑾璀璁璃璇璈璋璎璐璘璜璞璟璠璧璨璩璪璺瓒瓘瓜瓠瓢瓣瓤瓦瓮瓯瓴瓶瓷瓿甄甍甏甑甘甚甜生甥用甩甫甬甭田由甲申电男甸町画甾畀畅畈畋界畎畏畔留畚畛畜略畦番畲畴畸畹畿疃疆疏疑疔疖疗疙疚疝疟疠疡疣疤疥疫疬疭疮疯疱疲疳疴疵疸疹疼疽疾痂病症痈痉痊痍痒痔痕痘痛痞痢痣痤痦痧痨痪痫痰痱痴痹痼痿瘁瘃瘆瘊瘌瘐瘘瘙瘛瘟瘠瘢瘤瘦瘩瘪瘫瘰瘳瘴瘵瘸瘼瘾瘿癃癌癍癔癖癜癞癣癫癯癸登白百皂的皆皇皈皋皎皑皓皖皙皤皮皱皲皴皿盂盅盆盈盉益盍盎盏盐监盒盔盖盗盘盛盟盥目盯盱盲直相盹盼盾省眄眇眈眉眊看眍眙真眠眦眨眩眬眭眯眵眶眷眸眺眼着睁睃睄睇睐睑睚睛睡睢督睥睦睨睫睬睹睽睾睿瞀瞄瞅瞋瞌瞎瞑瞒瞟瞠瞢瞥瞧瞩瞪瞬瞭瞰瞳瞻瞽瞿矍矗矛矜矢矣知矩矫矬短矮石矶矸矾矿砀码砂砌砍砒研砖砗砘砚砜砝砟砣砥砧砭砰破砷砸砹砺砻砼砾础硅硇硌硎硐硒硕硖硗硝硫硬硭确硼碇碉碌碍碎碑碓碗碘碚碛碜碟碡碣碧碰碱碲碳碴碾磁磅磊磋磐磔磕磙磨磬磲磴磷礁礅礓礴示礼社祀祁祆祇祈祉祎祓祖祗祚祛祜祝神祟祠祢祥票祭祯祷祸祺祾禀禁禄禅禊福禧禳禹禺离禽禾秀私秃秆秉秋种科秒秕秘租秣秤秦秧秩秫秭积称秸移秽秾稀稂稃程稍税稔稗稚稞稠稣稳稷稻稼稽稿穆穑穗穰穴究穷穹空穿突窃窄窈窍窑窒窕窖窗窘窜窝窟窠窣窥窦窨窳窸窿立竑竖站竞竟章竣童竦竭端竹竺竽竿笃笄笆笈笊笋笏笑笔笕笙笛笞笠笤笥符笨笪第笮笳笸笺笼笾筇等筋筌筏筐筑筒答策筚筛筝筠筢筮筱筲筵筷筹签简箅箍箐箓箔箕算箜管箢箦箧箩箪箫箬箭箱箴箸篁篆篇篌篑篓篙篝篡篥篦篪篮篱篷篼篾簇簋簌簖簟簧簪簸簿籀籁籍米籴类籼籽粉粑粒粕粗粘粜粝粞粟粤粥粪粮粱粲粳粹粼粽精粿糁糅糊糌糍糕糖糗糙糜糟糠糨糯系紊素索紧紫累絮綦綮縠縻繁繇纂纛纠纡红纣纤纥约级纨纪纫纬纭纯纰纱纲纳纴纵纶纷纸纹纺纽纾线绀绁绂练组绅细织终绉绊绌绍绎经绑绒结绔绕绗绘给绚绛络绝绞统绠绡绢绣绥绦继绨绩绪绫续绮绯绰绱绲绳维绵绶绷绸绺绻综绽绾绿缀缁缂缃缄缅缆缇缈缉缌缎缑缒缓缔缕编缗缘缙缚缛缜缝缟缠缡缢缣缤缥缦缧缨缩缪缫缬缭缮缯缰缱缲缳缴缵缶缸缺罂罄罅罐网罔罕罗罘罚罟罡罢罨罩罪置罱署罴罹罽罾羁羊羌美羑羔羚羞羟羡群羧羯羰羲羸羹羼羽羿翁翅翊翌翎翔翕翘翚翟翠翡翥翩翮翰翱翳翻翼耀老考耄者耆耋而耍耐耒耕耖耗耘耙耜耠耢耥耦耧耨耩耪耱耳耶耷耸耻耽耿聂聃聆聊聋职聒联聘聚聩聪聱聿肃肄肆肇肉肋肌肓肖肘肚肛肝肟肠股肢肤肥肩肪肫肮肯肱育肴肺肼肽肾肿胀胁胂胃胄胆背胍胎胖胗胙胚胛胜胝胞胡胤胥胧胨胪胫胬胭胯胰胱胳胴胶胸胺胼能脂脆脉脊脍脏脐脑脒脓脔脖脘脚脬脯脱脲脸脾腆腈腊腋腌腐腑腓腔腕腚腠腥腧腩腭腮腰腱腴腹腺腻腼腾腿膀膂膈膊膏膑膘膙膛膜膝膦膨膳膺膻臀臂臃臆臊臌臜臣臧自臬臭至致臻臼臾舀舂舄舅舆舌舍舐舒舔舛舜舞舟舢航舫般舰舱舴舵舶舷舸船舻艄艇艋艏艘艟艨艮良艰色艳艺艽艾艿节芄芊芋芍芎芑芒芗芙芜芝芟芡芥芦芨芩芪芫芬芭芮芯花芳芷芸芹芼芽芾苁苄苇苈苋苌苍苎苏苑苒苓苔苕苗苘苛苜苞苟苡苣苤若苦苫苯英苴苷苹苻茀茁茂范茄茅茆茉茌茎茏茑茓茔茕茗茚茜茧茨茫茬茭茯茱茴茵茶茸茹茼荀荃荆荇草荏荐荑荒荔荚荜荞荟荠荡荣荤荥荦荧荨荩荪荫荬荮药荷荸荻荼荽莅莆莉莎莒莓莘莛莜莞莠莨莩莪莫莱莲莳莴获莸莹莺莼莽菀菁菅菇菊菌菏菔菖菘菜菟菠菡菩菪菰菱菲菹菽萁萃萄萋萌萍萎萏萑萘萜萝萤营萦萧萨萱萸萼落葆葑著葚葛葡董葩葫葬葭葱葳葵葶葸葺蒂蒋蒌蒗蒙蒜蒟蒡蒯蒲蒴蒸蒹蒺蒽蒿蓁蓄蓉蓊蓍蓐蓑蓓蓖蓝蓟蓠蓥蓦蓬蓼蓿蔑蔓蔗蔚蔟蔡蔫蔬蔷蔸蔺蔻蔼蔽蕃蕈蕉蕊蕖蕙蕞蕤蕨蕲蕴蕺蕻蕾薄薅薇薏薛薜薤薨薪薮薯薰薷薹藁藉藏藐藓藕藜藠藤藩藻藿蘅蘑蘖蘧蘩蘸虎虏虐虑虔虚虞虢虫虬虮虱虹虺虻虼虽虾虿蚀蚁蚂蚊蚋蚌蚍蚓蚕蚜蚝蚣蚤蚧蚨蚩蚪蚬蚯蚰蚱蚴蚶蛀蛄蛆蛇蛉蛊蛋蛎蛏蛐蛔蛘蛙蛛蛞蛟蛤蛩蛭蛮蛰蛱蛲蛳蛴蛸蛹蛾蜀蜂蜃蜇蜈蜉蜊蜍蜒蜓蜕蜗蜘蜚蜜蜞蜡蜢蜣蜥蜩蜮蜱蜴蜷蜻蜿蝇蝈蝉蝌蝎蝓蝗蝙蝠蝣蝥蝮蝰蝴蝶蝻蝼蝽蝾螂螃螅螈螋融螟螠螨螫螬螭螯螳螵螺螽蟀蟆蟊蟋蟑蟒蟛蟠蟥蟪蟮蟹蟾蠊蠓蠕蠖蠡蠢蠲蠹血衄衅行衍衔街衙衡衢衣补表衩衫衬衮衰衲衷衽衾衿袁袂袄袅袈袋袍袒袖袜袢袤被袭袱袷裁裂装裆裉裎裒裔裕裘裙裟裢裤裥裨裰裱裳裴裸裹裾褂褊褐褒褓褙褚褛褟褡褥褪褫褰褴褶襁襄襞襟襦襻西要覃覆见观规觅视觇览觉觊觋觎觏觐觑角觚觞解觥触觫觯觳言訇訚訾詈詹誉誊誓謇警譬计订讣认讥讦讧讨让讪讫训议讯记讲讳讴讵讶讷许讹论讼讽设访诀证诂诃评诅识诈诉诊诋诌词诏译诒诓诔试诖诗诘诙诚诛诜话诞诟诠诡询诣诤该详诧诨诩诫诬语诮误诰诱诲诳说诵请诸诹诺读诼诽课诿谀谁谂调谄谅谆谇谈谊谋谌谍谎谏谐谑谒谓谔谕谖谗谙谚谛谜谝谟谠谡谢谣谤谥谦谧谨谩谪谬谭谮谯谰谱谲谳谴谵谶谷豁豆豇豉豌豕豚象豢豨豪豫豳豸豹豺貂貅貉貊貌貔貘贝贞负贡财责贤败账货质贩贪贫贬购贮贯贰贱贲贳贴贵贶贷贸费贺贻贼贽贾贿赁赂赃资赅赇赈赉赊赋赌赍赎赏赐赓赔赕赖赘赙赚赛赜赝赞赟赠赡赢赣赤赦赧赫赭走赳赴赵赶起趁趄超越趋趑趔趟趣趱足趴趵趸趺趾趿跃跄跆跋跌跎跏跐跑跖跗跚跛距跞跟跣跤跨跪跬路跳践跶跷跸跹跺跻踅踉踊踌踏踒踔踝踞踟踢踩踪踬踮踯踱踵踹踺踽蹀蹁蹂蹄蹇蹈蹉蹊蹋蹑蹒蹙蹚蹦蹩蹬蹭蹰蹲蹴蹶蹼蹽蹾蹿躁躅躇躏躐躜躞身躬躯躲躺车轧轨轩轫转轭轮软轰轱轲轳轴轶轸轻轼载轾轿辂较辄辅辆辇辈辉辊辋辍辎辏辐辑输辔辕辖辗辘辙辚辛辜辞辟辣辨辩辫辰辱边辽达迁迂迄迅过迈迎运近迓返迕还这进远违连迟迢迤迥迦迨迩迪迫迭迮述迷迸迹追退送适逃逄逅逆选逊逋逍透逐逑递途逖逗通逛逝逞速造逡逢逦逮逯逵逶逸逻逼逾遁遂遄遇遍遏遐遑遒道遗遛遢遣遥遨遭遮遴遵遽避邀邂邃邈邋邑邓邕邗邙邛邝邢那邦邪邬邮邯邰邱邳邴邵邶邸邹邺邻邾郁郄郅郇郊郎郏郑郓郗郛郜郝郡郢郤郦郧部郫郭郯郴郸都郾郿鄂鄄鄙鄞鄢鄯鄱酃酆酉酊酋酌配酐酒酗酚酝酞酡酢酣酤酥酩酪酬酮酯酰酱酵酶酷酸酹酽酿醅醇醉醋醌醍醐醒醚醛醢醪醮醯醴醺采釉释里重野量金釜鉴銮鋆鋈錾鍪鎏鏊鏖鐾鑫钇针钉钊钋钌钍钎钏钒钓钕钗钙钚钛钝钞钟钠钡钢钣钤钥钦钧钨钩钫钮钯钰钱钲钳钴钵钹钺钻钼钽钾钿铀铁铂铃铄铅铆铈铉铊铋铌铍铎铐铑铒铗铙铛铜铝铟铠铡铢铣铤铧铨铩铪铫铬铭铮铯铰铱铲铳铵银铷铸铺铻铼链铿销锁锂锃锄锅锆锇锈锉锋锌锏锐锑锒锔锕锗错锚锛锜锝锞锟锡锢锣锤锥锦锨锩锭键锯锰锱锲锴锵锶锷锸锹锻镀镁镂镇镉镊镌镍镏镐镑镒镓镔镖镗镘镚镛镜镝镞镠镡镢镣镦镧镩镪镫镬镭镯镰镲镳镴镵镶长门闩闪闭问闯闰闱闲闳间闵闷闸闹闺闻闼闽闾闿阀阁阂阃阄阅阆阇阈阉阊阋阍阎阏阐阑阒阔阕阖阗阙阚阜队阡阪阮阱防阳阴阵阶阻阿陀陂附际陆陇陈陉陋陌降限陔陕陛陟陡院除陨险陪陬陲陵陶陷隅隆隈隋隍随隐隔隗隘隙障隧隰隳隶隼隽难雀雁雄雅集雇雉雌雍雎雏雒雕雠雨雩雪雯雱雳零雷雹雾需霁霄霆震霈霉霍霎霏霓霖霜霞霨霪霭霰露霸霹霾青靓靖静靛非靠靡面靥革靰靳靴靶靸靺靼靿鞅鞋鞍鞑鞒鞘鞠鞡鞣鞧鞨鞫鞬鞭鞯鞲鞴韦韧韩韪韫韬韭音韵韶页顶顷顸项顺须顼顽顾顿颀颁颂颃预颅领颇颈颉颊颌颍颎颏颐频颓颔颖颗题颙颚颛颜额颞颟颠颡颢颤颦颧风飑飒飓飕飘飙飞食飧飨餍餐餮饔饕饥饧饨饩饪饫饬饭饮饯饰饱饲饴饵饶饷饸饹饺饼饽饿馁馃馄馅馆馇馈馊馋馍馏馐馑馒馓馔馕首馗香馥馨马驭驮驯驰驱驳驴驶驷驸驹驻驼驽驾驿骀骁骂骄骅骆骇骈骊骋验骎骏骐骑骒骓骖骗骘骚骛骜骝骞骟骠骡骢骣骤骥骧骨骰骶骷骸骺骼髀髁髂髅髋髌髑髓高髡髦髫髭髯髹髻鬃鬈鬏鬓鬟鬣鬲鬻鬼魁魂魃魄魅魆魇魈魉魍魏魑魔鱼鱿鲀鲁鲂鲅鲆鲇鲈鲋鲍鲎鲐鲑鲔鲚鲛鲜鲞鲟鲠鲡鲢鲣鲤鲥鲧鲨鲩鲫鲮鲱鲲鲳鲴鲵鲷鲸鲻鲼鲽鳀鳃鳄鳅鳇鳊鳌鳍鳎鳏鳐鳓鳔鳕鳖鳗鳙鳜鳝鳞鳟鳢鸟鸠鸡鸢鸣鸥鸦鸨鸩鸪鸫鸬鸭鸮鸯鸱鸲鸳鸵鸶鸷鸸鸹鸻鸽鸾鸿鹁鹂鹃鹄鹅鹆鹇鹈鹉鹊鹋鹌鹏鹐鹑鹕鹗鹘鹚鹛鹜鹞鹤鹦鹧鹩鹪鹫鹬鹭鹮鹰鹳鹿麂麇麈麋麒麓麝麟麦麸麻麽麾黄黍黎黏黑黔默黛黜黝黟黠黢黥黧黩黪黯黻黼黾鼋鼍鼎鼐鼓鼙鼠鼢鼬鼯鼹鼻鼾齁齉齐齑齿龀龃龄龅龇龈龉龊龋龌龙龚龛龟龠！（），：？𠳐𥻗𬉼가각간갇갈감갑값갓갔강갖같갚갛개객갤갱걀걔거걱건걷걸검겁것겉게겟겠겨격겪견결겹겼경곁계고곡곤곧골곰곱곳공과곽관광괜괴굉교구국군굳굴굵굶굽궁권궤귀귓규균귤그극근글긁금급긋긍기긴길김깃깅깊까깍깎깐깔깜깝깡깥깨꺅꺼꺾껌껍껏께껴꼬꼭꼴꼼꼽꽂꽃꽉꽝꽤꾀꾸꾼꿀꿈뀌끄끈끊끌끓끔끗끝끼낄낌나낙낚난날낡남납낫났낭낮낯낱낳내낼냄냅냇냈냉냐냥너넉넌널넓넘넣네넥넷녀녁년념녕녘노녹논놀놈농높놓놔뇌뇨누눈눌눕뉘뉴늄느늑는늘늙능늦늬니닉닌닐님닙닛닝다닥닦단닫달닭닮담답닷당닿대댁댐댓더덕던덜덟덤덥덧덩덮데델도독돈돌돔돕돗동돛돼됐되된될됨됩두둑둔둘둠둡둥뒤뒷듀듈드득든듣들듬듭듯등디딕딜딧딨딩딪따딸땀땅때떠떡떤떨떱떴떻떼또똑뚝뚫뚱뛰뜨뜩뜹뜻라락란람랍랑랗래랙랜램랫략량러럭런럴럼럽럿렁렇레렉렌렘렛려력련렬렴렵렸령례로록론롤롬롭롯뢰료루룩룹룻뤄류륙률륨륭르른를름릅릇릎리릭린릴림립릿링마막만많말맑맘맙맛망맞맡맣매맥맨맬맵맹맺머먹먼멀멈멋멍멎메멘며면멸명몇모목몬몰몸몹못몽묘무묵묶문묻물뭄뭇뭐뭔뭘뭣뮤므미민믿밀밉밌및밑바박밖반받발밝밟밤밥밧방밭배백밴밸뱀뱃뱅뱉버번벌범법벗베벤벨벼벽변별볍병볕보복볶본볼봄봅봇봉봐뵈뵙부북분불붉붐붑붓붕붙뷰브븐블비빅빈빌빔빕빗빙빚빛빠빡빨빵빼뺏뺨뻐뻔뻗뼈뼉뽑뾰뿌뿐뿔뿜쁘쁨사삭산살삶삼삿상새색샌샘생샤샵샷서석섞선설섬섭섯성세섹센셀셈셉셋셔션셨소속손솔솜솟송솥쇄쇠쇼수숙순숟술숨숫숭숲쉘쉬쉰쉽슈슐스슨슬슴습슷승시식신싣실싫심십싯싱싶싸싹싼쌀쌍쌓써썩썰썹쎄쏘쏟쏩쏴쑤쑥쓰쓴쓸씀씌씨씩씬씹씻아악안앉않알앓암압앗았앙앞애액앤앨앱앵야약얀얄얇얌양얕얗얘어억언얹얻얼엄업없엇었엉엊엌엎에엑엔엘여역연열엷염엽엿였영옆예옛오옥온올옮옳옵옷옹와완왓왔왕왜왠외왼요욕용우욱운울움웃웅워원월웠웨웬웰웸위윈윌윗유육윤율으윽은을음응의이익인일읽잃임입잇있잉잊잎자작잔잖잘잠잡잣장잦재잿쟁쟤저적전절젊점접젓정젖제젠젤젯져졌조족존졸좀좁종좋좌죄주죽준줄줌줍중줘쥐즈즉즌즐즘증지직진질짐집짓징짙짚짜짝짧째쨌쩌쩍쩐쩔쩜쪽쫓쭈쭉쯤찌찍찢차착찬찮찰참찻찼창찾채책챈챌챔챙처척천철첨첩첫청체쳐쳤초촉촌촛총촬최추축춘출춤춥춧충춰취츠측츰층치칙친칠침칫칭카칸칼캄캐캠캡커컨컬컴컵컷케켄켓켜켰코콘콜콤콩쾌쿄쿠쿰쿼퀄퀘퀴퀵큐크큰클큼큽키킬킵킷타탁탄탈탐탑탓탕태택탠탤탬탭탱터턱턴털텀텅테텍텐텔템토톤톨톰톱통퇴투툴툼퉁튀튕튜트특튼튿틀틈티틱틸팀팅파팎판팔팝패팩팬퍼퍽펀펄펑페펜펠펴편펼평폐포폭폰폴폼표푸푹풀품풍퓨프플픔피픽핀필핏핑하학한할함합항해핵핸햄햇했행향허헌험헤헬혀혁현혈협혔형혜호혹혼홀홈홉홍화확환활황회획횟횡효후훈훌훔훨휘휴흉흐흑흔흘흙흡흥흩희흰히힌힘힙";
	public static final String FILE_SAFE_ALPHABET = " qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890-=+_!,'@£$%^&()±ієїґІЄЇҐåäöüÄÖÜÉÊÈéêèëÇçÅÀàâûąćęłńóśżźĄĆĘŁŃÓŚŻŹАаБбВвГгДдЕеЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЬьЮюЯяЫыЭэЪъЁё`¡¿ÁáíñúđďþňčřžšßťǎăāæěĕēòôøœůÿïĐĎÞÑŇČŘŽŠSSŤÂǍĂĀÆËÂĚĔĒÒÔØŒŮŸÏÍ”…　、。々あいうえおかがきぎくぐけげこごさしじすずせぜそぞただちっつづてでとどなにぬねのはばひびぶへべほぼぽまみむめもゃやゅゆょよらりるれろわをんァアィイウェエォオカガキギクグケゲコゴサザシジスズセゼソゾタダチッツテデトドナニネノハバパヒビピフブプヘベペホボポマミムメモャヤュユョラリルレロワンヴ・ー一万丈三上下不与世両並中丸主久乗乱乾了予争事二互亡交人今介仕他付代令以仲件任伏伝伴伸似位低住体何作使供依価侵係保信修倉個倍倒値停側偵備催債傷働像儀償優元充先光党入全公共兵具内円再写冠冶処凹出刃分切刑初判別利到制刻削前剖副創力功加劣助努労効勇動務勝勢包化北匹区医匿十半卒協南単占印危即卵厚厳去参及反収取受口古句叩可台右司各合同名向君否含吸吹告周味呼命和品員哨哲唆唱唾商問善喜喰営噂器四回団困囲図固国土圧在地垂型城域基報場塔塗塞塩填境増墜壁壊壌士壮売変夕外多夜大太夫央失奇契奨奪奴好如妙妨始威子字存学孵守安完宗官定宝実客宣室害家容宿寄密富寝察寸寺対専射将尋導小少尖尽尾局居屈屋展属層屯山岩峙島崇崩嵐巣工左巧巨市帆希帝帯帰常幅干平年幸幹広底店度座庫延建式引弱張強弾当形彫影役彼征径待後徐従得御復徹心必忘応快念怖思急性怪恐悟悪情惨意感態慎憶懇懸成我戒戦戻所扇扉手打払扱技抑投抗折抜択抵押抽担拒拠拡拿持指挑振挺捉捕捜捨捻授排掘掛採探接推掲掴揃描提揚換揮援揺損搬搭搾撃撤操支改攻放政故救敗教敢散数整敵敷文料斜断新方施旅旋旗既日旧早昆昇明易星昼時晴晶暮暴曲更書替最有服望期木未末本材村杖条来東板析林果枝枠枯柔査栄根格案梯械棄棚森検業極楽構様槽標模権樫樽機欠次欲歓止正武歩死残殴段殺殻殿毎毒比民気水永求池決治況泊法注洋洗活派流浅浪浮海消深渇済減測満源準溶滅滴漏演漬潜火灰災炉炎炭炸点為烈無焦然焼煉煙照煽熟熱燃燥爆片牛物牲特犠犯状狂狙狩独猛献獲率王珍現球理璧環瓦生産用田由甲申画界留畜略番異疑療発登白的皆皮皿益盗盟監盤目直相真眠着瞬矢知短石研砕砲破硬確磨礎示祈神禁秀私科秒秘移程税種稼積穏穴究空突窓窮立竜章端笏笑笛第筒策箔算管節範築簡粉粒粗精約紅紋納純級素索細紹終組絆経結絞給統絶継続維網綻緑線編緩練縦縮績繁繋織繰罪置羅美群義翻翼考者耐肉肢育胆背能脅脆脚脱腐腹膠臨自臭至致舎航舷船艇艘艦良色芸苦茂草荒荷莫落葉著蒸蓄蔵薄薬蘇虐虫融血行術街衛衝衡表被裁裂装裏補製複襲西要覇見規視覚覧観角解触言計訓記訣訪設許訳証詠試詰話詳誇認語説読誰調諜謝識警議護豊象負財貢貨販貪貫貯買費貼賂賄資賊質購贈赤走起超足距跡路跳身車軌軍軟転軽較載輝輪輸辺込辿迎近返迫追退送逃逆透逐途通速造連週進遅遇遊運過道達遙遠遣適遭選避邪部都配酬酷酸醸重野量金鉄鉱銀銃銅鋭鋼錆錬録鍮鎖鎮鏡長門閉開間関闇闘防阻降限院除陥陸険陽隊階際障隠隣集雇離難雨雪雷電震霧露青非面革鞄音響頃項順頑頓領頭頻頼題願類飛食飾養餌首駄駆駐騎騒験驚骨骸高魔魚黄黒！％＆（）＋－０１２３４５６７８：？ｓ～ｽﾃﾅﾒﾝ—“”…、。《》㤘㧐㧟㸆䁖䏝䥽䦃一丁七万丈三上下不与丏丐丑专且丕世丘丙业丛东丝丞丢两严丧个丫中丰串临丸丹为主丽举乂乃久么义之乌乍乎乏乐乒乓乔乖乘乙乜九乞也习乡书乩买乱乳乾了予争事二于亏云互亓五井亘亚些亟亡亢交亥亦产亨亩享京亭亮亲亳亵亶人亿什仁仂仃仄仅仆仇仉今介仍从仑仓仔仕他仗付仙仞仟仡代令以仨仪仫们仰仲仵件价任份仿企伉伊伍伎伏伐休众优伙会伛伞伟传伢伤伥伦伧伪伫伯估伴伶伸伺似伽佃但位低住佐佑体何佗佘余佚佛作佝佞佟你佣佤佥佩佬佯佰佳佶佻佼佾使侃侄侈侉例侍侏侑侔侗供依侠侣侥侦侧侨侩侪侬侮侯侵便促俄俅俊俎俏俐俑俗俘俚保俞俟信俣俦俨俩俪俭修俯俱俳俵俶俸俺俾倌倍倏倒倔倘候倚倜借倡倥倦倨倩倪倬倭债值倾偃假偈偌偎偏偕做停健偬偶偷偻偾偿傀傅傈傍傣傥傧储傩催傲傻像僖僚僧僭僮僰僳僵僻儆儋儒儡儿兀允元兄充兆先光克免兑兔兕兖党兜兢入全八公六兮兰共关兴兵其具典兹养兼兽冀内冈冉册再冒冕冗写军农冠冢冤冥冬冯冰冲决况冶冷冻冼冽净凄准凇凉凋凌减凑凛凝几凡凤凫凭凯凰凳凶凸凹出击凼函凿刀刁刃分切刈刊刍刎刑划刖列刘则刚创初删判刨利别刭刮到刳制刷券刹刺刻刽刿剀剁剂剃削剋剌前剐剑剔剖剜剞剡剥剧剩剪副割剽剿劁劈劓力劝办功加务劢劣动助努劫劬劭励劲劳劼劾势勃勇勉勋勐勒勖勘募勤勰勺勾勿匀包匆匈匍匏匐匕化北匙匜匝匠匡匣匦匪匮匹区医匾匿十千卅升午卉半华协卑卒卓单卖南博卜卞卟占卡卢卣卤卦卧卫卮卯印危即却卵卷卸卿厂厄厅历厉压厌厍厕厘厚厝原厢厥厦厨厩厮去厾县叁参叆又叉及友双反发叔取受变叙叛叟叠口古句另叨叩只叫召叭叮可台叱史右叵叶号司叹叻叼叽吁吃各吆合吉吊同名后吏吐向吓吕吗君吝吞吟吠吡吣否吧吨吩含听吭吮启吱吲吴吵吸吹吻吼吽吾呀呃呆呈告呋呐呒呓呔呕呖呗员呛呜呢呣呤呦周呱呲味呵呶呷呸呻呼命咀咂咄咆咋和咎咏咐咒咔咕咖咙咚咛咝咣咤咦咧咨咩咪咫咬咯咱咳咴咸咻咽咿哀品哂哄哆哇哈哉哌响哎哏哐哑哓哔哕哗哙哚哝哞哟哥哦哧哨哩哪哭哮哲哺哼哽唁唆唇唉唏唐唑唔唛唠唢唣唤唧唬售唯唰唱唳唵唷唼唾唿啁啃啄商啉啊啐啕啖啜啡啤啥啦啧啪啬啭啮啰啵啶啷啸啻啼啾喀喁喂喃善喇喈喉喊喋喏喑喔喘喙喜喝喟喧喱喳喵喷喹喻喽喾嗄嗅嗉嗌嗍嗐嗑嗒嗓嗔嗖嗜嗝嗞嗟嗡嗣嗤嗥嗦嗨嗪嗫嗬嗯嗲嗳嗵嗷嗽嗾嘀嘁嘈嘉嘌嘎嘘嘚嘛嘞嘟嘡嘣嘤嘧嘬嘭嘱嘲嘴嘶嘹嘻嘿噌噍噎噔噗噘噙噜噢噤器噩噪噫噬噱噶噻噼嚄嚅嚆嚎嚏嚓嚣嚯嚷嚼囊囔囚四回囟因囡团囤囫园困囱围囵囹固国图囿圃圄圆圈圉圊圜土圣在圩圪圬圭圮圯地圳圹场圻圾址坂均坊坌坍坎坏坐坑块坚坛坝坞坟坠坡坤坦坨坩坪坫坭坯坳坷坻坼垂垃垄垆型垌垒垓垛垠垡垢垣垤垦垧垩垫垭垮垱垸埂埃埋城埒埔埕埘埙埚埝域埠埤埭埯埴埵埸培基埽堂堆堇堉堋堍堑堕堙堞堡堤堪堰堵塄塆塌塍塑塔塘塞填塬塾墀墁境墅墉墒墓墙增墟墨墩墼壁壅壑壕壤士壬壮声壳壶壹处备复夏夔夕外夙多夜够夤大天太夫夬夭央夯失头夷夸夹夺夼奁奂奄奇奈奉奋奎奏契奔奕奖套奘奚奠奢奥奭女奴奶奸她好妁如妃妄妆妇妈妊妍妒妓妖妗妙妞妣妤妥妨妩妪妫妮妯妲妹妻妾姆姊始姐姑姒姓委姗姘姚姜姝姣姥姨姬姮姹姻姿威娃娄娅娆娇娈娉娌娑娓娘娜娟娠娣娥娩娱娲娴娶娼婀婆婉婊婕婚婢婧婪婴婵婶婷婺婿媒媚媛媪媭媲媳媵媸媾嫁嫂嫉嫌嫒嫔嫖嫘嫚嫠嫡嫣嫦嫩嫫嫱嬉嬖嬗嬴嬷孀子孑孓孔孕字存孙孚孛孜孝孟孢季孤孥学孩孪孬孰孱孳孵孺孽宁它宅宇守安宋完宏宓宕宗官宙定宛宜宝实宠审客宣室宥宦宪宫宰害宴宵家宸容宽宾宿寂寄寅密寇富寐寒寓寝寞察寡寤寥寨寮寰寸对寺寻导寿封射将尉尊小少尔尕尖尘尚尜尝尤尥尧尬就尴尸尹尺尻尼尽尾尿局屁层居屈屉届屋屎屏屐屑展屙属屠屡屣履屦屯山屹屺屿岁岂岈岌岐岑岔岖岗岘岚岛岢岣岩岫岬岭岱岳岷岸岿峁峄峋峒峙峡峣峤峥峦峨峪峭峰峻崂崃崆崇崎崔崖崚崛崤崦崩崭崮崴崽嵇嵊嵋嵌嵎嵖嵘嵛嵝嵩嵫嵬嵯嵴嶂嶙嶝嶷巅巉巍川州巡巢工左巧巨巩巫差巯己已巳巴巷巽巾币市布帅帆师希帏帐帑帔帕帖帘帙帚帛帜帝带帧席帮帷常帻帼帽幂幄幅幌幔幕幛幞幡幢干平年并幸幺幻幼幽广庄庆庇床庋序庐庑库应底庖店庙庚府庞废庠庥度座庭庵庶康庸庹庾廉廊廓廖廛廨廪延廷建廿开弁异弃弄弈弊弋式弑弓引弗弘弛弟张弥弦弧弩弭弯弱弹强弼彀归当录彗彘彝形彤彦彧彩彪彬彭彰影彷役彻彼往征徂径待徇很徉徊律後徐徒徕得徘徙徜御徨循徭微徵德徼徽心必忆忌忍忏忐忑忒忖志忘忙忝忠忡忤忧忪快忭忱念忸忻忽忾忿怀态怂怃怄怅怆怍怎怏怒怔怕怖怙怛怜思怠怡急怦性怨怩怪怫怯怵总怼怿恁恂恃恋恍恐恒恓恕恙恚恢恣恤恨恩恪恫恬恭息恰恳恶恸恹恺恻恼恽恿悄悉悌悍悒悔悖悚悛悝悟悠患悦您悫悬悭悯悱悲悴悸悻悼情惆惇惊惋惑惕惘惚惜惝惟惠惦惧惨惩惫惬惭惮惯惰想惴惶惹惺愀愁愆愈愉愎意愔愕愚感愠愣愤愦愧愫愿慈慊慌慎慑慕慝慢慧慨慰慵慷憋憎憔憧憨憩憬憾懂懈懊懋懑懒懦懵懿戆戈戊戌戍戎戏成我戒戕或戗战戚戛戟戡戢戥截戬戮戳戴户戽戾房所扁扃扇扈扉手才扎扑扒打扔托扛扣扦执扩扪扫扬扭扮扯扰扳扶批扼找承技抃抄抉把抑抒抓抔投抖抗折抚抛抟抠抡抢护报抨披抬抱抵抹抻押抽抿拂拃拄担拆拇拈拉拊拌拍拎拐拒拓拔拖拗拘拙拚招拜拟拢拣拤拥拦拧拨择括拭拮拯拱拳拴拷拼拽拾拿持挂指挈按挎挑挖挚挛挝挞挟挠挡挣挤挥挦挨挪挫振挲挹挺挽捂捃捅捆捉捋捌捍捎捏捐捕捞损捡换捣捧捩捭据捯捶捷捺捻捽掀掂掇授掉掊掌掏掐排掖掘掠探掣接控推掩措掬掮掰掳掴掷掸掺掼掾揄揆揉揍描提插揖揠握揣揩揪揭揳援揶揸揽揿搀搁搂搅搋搏搐搓搔搛搜搞搠搡搦搪搬搭搴携搽摁摄摅摆摇摈摊摒摔摘摞摧摩摭摸摹摽撂撄撅撇撑撒撕撙撞撤撩撬播撮撰撵撷撸撺撼擀擂擅操擎擒擘擞擢擤擦攀攉攒攘攥攫攮支收攸改攻放政故效敌敏救敕敖教敛敝敞敢散敦敫敬数敲整敷文斋斌斐斑斓斗料斛斜斟斡斤斥斧斩斫断斯新方於施旁旃旄旅旆旋旌旎族旒旖旗无既日旦旧旨早旬旭旮旯旰旱时旷旸旺旻昀昂昃昆昉昊昌明昏易昔昕昙昝星映春昧昨昭是昱昴昵昶昼显晁晃晋晌晏晒晓晕晗晚晞晟晡晤晦晨普景晰晴晶晷智晾暂暄暇暌暑暖暗暝暧暨暮暴暹暾曙曛曜曝曦曩曰曲曳更曷曹曼曾替最月有朋服朐朔朕朗望朝期朦木未末本札术朱朴朵机朽杀杂权杆杈杉杌李杏材村杓杖杜杞束杠条来杨杪杭杯杰杲杳杵杷杻杼松板极构枇枉枋析枕林枚果枝枞枢枣枥枧枨枪枫枭枯枰枳枵架枷枸柁柄柈柏某柑柒染柔柘柙柚柜柝柞柠柢查柩柬柯柰柱柳柴柽柿栀栅标栈栉栊栋栌栎栏树栓栖栗栝栟校栩株栲栳样核根格栽栾桀桁桂桃桅框案桉桌桎桐桑桓桔桕桡桢档桤桥桦桧桨桩桫桴桶桷梁梃梅梆梏梓梗梢梦梧梨梭梯械梳梵梿检棁棂棉棋棍棒棕棘棚棠棣森棰棱棵棹棺棻棼椁椅椋植椎椐椒椟椠椤椪椭椰椴椽椿楂楔楚楝楠楣楦楫楮楯楷楸楹楼概榄榆榈榉榔榕榖榛榜榧榨榫榭榱榴榷榻槁槊槌槎槐槔槛槟槠槭槲槽槿樊樗樘樟模樨横樯樱樵樽樾橄橇橐橘橙橛橡橱橹橼檀檄檎檐檗檠檩檬欠次欢欣欤欧欲欸欺款歃歆歇歉歌歙止正此步武歧歪歹死歼殁殂殃殄殆殇殉殊残殍殒殓殖殚殛殡殪殴段殷殿毁毂毅毋母每毒毓比毕毖毗毙毛毡毪毫毯毳毹毽氅氆氇氍氏氐民氓气氖氘氙氚氛氟氡氢氤氦氧氨氩氪氮氯氰氲水永汀汁求汆汇汉汊汐汔汕汗汛汜汝汞江池污汤汨汩汪汰汲汴汶汹汽汾沁沂沃沅沆沈沉沌沏沐沓沔沙沚沛沟没沣沤沥沦沧沨沩沪沫沭沮沱河沸油治沼沽沾沿泄泅泉泊泌泐泓泔法泖泗泛泞泠泡波泣泥注泪泫泮泯泰泱泳泵泷泸泺泻泼泽泾洁洄洇洋洌洎洒洗洙洛洞津洧洪洫洮洱洲洳洵洹活洼洽派流浃浅浆浇浊测浍济浏浑浒浓浔浕浙浚浜浞浠浣浥浦浩浪浮浯浴海浸涂涅消涉涌涎涑涓涔涕涛涝涞涟涠涡涣涤润涧涨涩涪涫涮涯液涵涸涿淀淄淅淆淇淋淌淑淖淘淙淝淞淠淡淤淦淫淬淮深淳混淹添清渊渌渍渎渐渑渔渗渚渝渠渡渣渤渥温渫渭港渲渴游渺湃湄湉湍湎湔湖湘湛湜湟湫湮湲湾湿溃溅溆溉溏源溘溜溟溢溥溧溪溯溱溲溴溶溷溺溻溽滁滂滃滇滋滏滑滓滔滕滗滚滞滟满滢滤滥滦滨滩滪滫滴滹漂漆漉漏漓演漕漠漩漪漫漭漯漱漳漶漾潇潋潍潘潜潞潟潢潦潭潮潲潴潸潺潼澄澈澉澌澍澎澜澡澥澧澳澶澹激濂濉濑濒濞濠濡濮濯瀑瀚瀛瀣灌灏灞火灭灯灰灵灶灸灼灾灿炀炅炉炊炎炒炔炕炖炘炙炜炝炫炬炭炮炯炱炳炷炸点炻炼炽烀烁烂烃烈烊烘烙烛烜烟烤烦烧烨烩烫烬热烯烷烹烽焉焊焐焓焕焖焗焘焙焚焜焦焯焰焱然煅煊煌煎煜煞煤煦照煨煮煲煳煸煺煽熄熊熏熔熘熙熜熟熠熥熨熬熵熹燃燎燔燕燠燥燧燮燹爆爝爨爪爬爰爱爵父爷爸爹爻爽爿牁牂片版牌牍牒牖牙牛牝牟牡牢牤牦牧物牮牯牲牵特牺牾犀犁犄犊犋犍犏犒犟犬犯犰状犷犸犹狁狂狄狈狍狎狐狒狗狙狞狠狡狨狩独狭狮狯狰狱狲狳狷狸狺狼猁猃猎猕猖猗猛猜猝猞猡猢猥猩猪猫猬献猱猴猷猹猾猿獐獒獗獠獬獭獴獾玄率玉王玎玑玕玖玙玛玠玡玢玥玦玩玫玭玮环现玲玳玷玺玻珀珂珈珉珊珍珏珐珑珙珞珠珣珥珧珩班珰珲球琅理琇琉琏琐琚琛琢琤琥琦琨琪琫琬琮琯琰琳琴琵琶琼瑁瑄瑕瑗瑙瑚瑛瑜瑞瑟瑭瑰瑶瑾璀璁璃璇璈璋璎璐璘璜璞璟璠璧璨璩璪璺瓒瓘瓜瓠瓢瓣瓤瓦瓮瓯瓴瓶瓷瓿甄甍甏甑甘甚甜生甥用甩甫甬甭田由甲申电男甸町画甾畀畅畈畋界畎畏畔留畚畛畜略畦番畲畴畸畹畿疃疆疏疑疔疖疗疙疚疝疟疠疡疣疤疥疫疬疭疮疯疱疲疳疴疵疸疹疼疽疾痂病症痈痉痊痍痒痔痕痘痛痞痢痣痤痦痧痨痪痫痰痱痴痹痼痿瘁瘃瘆瘊瘌瘐瘘瘙瘛瘟瘠瘢瘤瘦瘩瘪瘫瘰瘳瘴瘵瘸瘼瘾瘿癃癌癍癔癖癜癞癣癫癯癸登白百皂的皆皇皈皋皎皑皓皖皙皤皮皱皲皴皿盂盅盆盈盉益盍盎盏盐监盒盔盖盗盘盛盟盥目盯盱盲直相盹盼盾省眄眇眈眉眊看眍眙真眠眦眨眩眬眭眯眵眶眷眸眺眼着睁睃睄睇睐睑睚睛睡睢督睥睦睨睫睬睹睽睾睿瞀瞄瞅瞋瞌瞎瞑瞒瞟瞠瞢瞥瞧瞩瞪瞬瞭瞰瞳瞻瞽瞿矍矗矛矜矢矣知矩矫矬短矮石矶矸矾矿砀码砂砌砍砒研砖砗砘砚砜砝砟砣砥砧砭砰破砷砸砹砺砻砼砾础硅硇硌硎硐硒硕硖硗硝硫硬硭确硼碇碉碌碍碎碑碓碗碘碚碛碜碟碡碣碧碰碱碲碳碴碾磁磅磊磋磐磔磕磙磨磬磲磴磷礁礅礓礴示礼社祀祁祆祇祈祉祎祓祖祗祚祛祜祝神祟祠祢祥票祭祯祷祸祺祾禀禁禄禅禊福禧禳禹禺离禽禾秀私秃秆秉秋种科秒秕秘租秣秤秦秧秩秫秭积称秸移秽秾稀稂稃程稍税稔稗稚稞稠稣稳稷稻稼稽稿穆穑穗穰穴究穷穹空穿突窃窄窈窍窑窒窕窖窗窘窜窝窟窠窣窥窦窨窳窸窿立竑竖站竞竟章竣童竦竭端竹竺竽竿笃笄笆笈笊笋笏笑笔笕笙笛笞笠笤笥符笨笪第笮笳笸笺笼笾筇等筋筌筏筐筑筒答策筚筛筝筠筢筮筱筲筵筷筹签简箅箍箐箓箔箕算箜管箢箦箧箩箪箫箬箭箱箴箸篁篆篇篌篑篓篙篝篡篥篦篪篮篱篷篼篾簇簋簌簖簟簧簪簸簿籀籁籍米籴类籼籽粉粑粒粕粗粘粜粝粞粟粤粥粪粮粱粲粳粹粼粽精粿糁糅糊糌糍糕糖糗糙糜糟糠糨糯系紊素索紧紫累絮綦綮縠縻繁繇纂纛纠纡红纣纤纥约级纨纪纫纬纭纯纰纱纲纳纴纵纶纷纸纹纺纽纾线绀绁绂练组绅细织终绉绊绌绍绎经绑绒结绔绕绗绘给绚绛络绝绞统绠绡绢绣绥绦继绨绩绪绫续绮绯绰绱绲绳维绵绶绷绸绺绻综绽绾绿缀缁缂缃缄缅缆缇缈缉缌缎缑缒缓缔缕编缗缘缙缚缛缜缝缟缠缡缢缣缤缥缦缧缨缩缪缫缬缭缮缯缰缱缲缳缴缵缶缸缺罂罄罅罐网罔罕罗罘罚罟罡罢罨罩罪置罱署罴罹罽罾羁羊羌美羑羔羚羞羟羡群羧羯羰羲羸羹羼羽羿翁翅翊翌翎翔翕翘翚翟翠翡翥翩翮翰翱翳翻翼耀老考耄者耆耋而耍耐耒耕耖耗耘耙耜耠耢耥耦耧耨耩耪耱耳耶耷耸耻耽耿聂聃聆聊聋职聒联聘聚聩聪聱聿肃肄肆肇肉肋肌肓肖肘肚肛肝肟肠股肢肤肥肩肪肫肮肯肱育肴肺肼肽肾肿胀胁胂胃胄胆背胍胎胖胗胙胚胛胜胝胞胡胤胥胧胨胪胫胬胭胯胰胱胳胴胶胸胺胼能脂脆脉脊脍脏脐脑脒脓脔脖脘脚脬脯脱脲脸脾腆腈腊腋腌腐腑腓腔腕腚腠腥腧腩腭腮腰腱腴腹腺腻腼腾腿膀膂膈膊膏膑膘膙膛膜膝膦膨膳膺膻臀臂臃臆臊臌臜臣臧自臬臭至致臻臼臾舀舂舄舅舆舌舍舐舒舔舛舜舞舟舢航舫般舰舱舴舵舶舷舸船舻艄艇艋艏艘艟艨艮良艰色艳艺艽艾艿节芄芊芋芍芎芑芒芗芙芜芝芟芡芥芦芨芩芪芫芬芭芮芯花芳芷芸芹芼芽芾苁苄苇苈苋苌苍苎苏苑苒苓苔苕苗苘苛苜苞苟苡苣苤若苦苫苯英苴苷苹苻茀茁茂范茄茅茆茉茌茎茏茑茓茔茕茗茚茜茧茨茫茬茭茯茱茴茵茶茸茹茼荀荃荆荇草荏荐荑荒荔荚荜荞荟荠荡荣荤荥荦荧荨荩荪荫荬荮药荷荸荻荼荽莅莆莉莎莒莓莘莛莜莞莠莨莩莪莫莱莲莳莴获莸莹莺莼莽菀菁菅菇菊菌菏菔菖菘菜菟菠菡菩菪菰菱菲菹菽萁萃萄萋萌萍萎萏萑萘萜萝萤营萦萧萨萱萸萼落葆葑著葚葛葡董葩葫葬葭葱葳葵葶葸葺蒂蒋蒌蒗蒙蒜蒟蒡蒯蒲蒴蒸蒹蒺蒽蒿蓁蓄蓉蓊蓍蓐蓑蓓蓖蓝蓟蓠蓥蓦蓬蓼蓿蔑蔓蔗蔚蔟蔡蔫蔬蔷蔸蔺蔻蔼蔽蕃蕈蕉蕊蕖蕙蕞蕤蕨蕲蕴蕺蕻蕾薄薅薇薏薛薜薤薨薪薮薯薰薷薹藁藉藏藐藓藕藜藠藤藩藻藿蘅蘑蘖蘧蘩蘸虎虏虐虑虔虚虞虢虫虬虮虱虹虺虻虼虽虾虿蚀蚁蚂蚊蚋蚌蚍蚓蚕蚜蚝蚣蚤蚧蚨蚩蚪蚬蚯蚰蚱蚴蚶蛀蛄蛆蛇蛉蛊蛋蛎蛏蛐蛔蛘蛙蛛蛞蛟蛤蛩蛭蛮蛰蛱蛲蛳蛴蛸蛹蛾蜀蜂蜃蜇蜈蜉蜊蜍蜒蜓蜕蜗蜘蜚蜜蜞蜡蜢蜣蜥蜩蜮蜱蜴蜷蜻蜿蝇蝈蝉蝌蝎蝓蝗蝙蝠蝣蝥蝮蝰蝴蝶蝻蝼蝽蝾螂螃螅螈螋融螟螠螨螫螬螭螯螳螵螺螽蟀蟆蟊蟋蟑蟒蟛蟠蟥蟪蟮蟹蟾蠊蠓蠕蠖蠡蠢蠲蠹血衄衅行衍衔街衙衡衢衣补表衩衫衬衮衰衲衷衽衾衿袁袂袄袅袈袋袍袒袖袜袢袤被袭袱袷裁裂装裆裉裎裒裔裕裘裙裟裢裤裥裨裰裱裳裴裸裹裾褂褊褐褒褓褙褚褛褟褡褥褪褫褰褴褶襁襄襞襟襦襻西要覃覆见观规觅视觇览觉觊觋觎觏觐觑角觚觞解觥触觫觯觳言訇訚訾詈詹誉誊誓謇警譬计订讣认讥讦讧讨让讪讫训议讯记讲讳讴讵讶讷许讹论讼讽设访诀证诂诃评诅识诈诉诊诋诌词诏译诒诓诔试诖诗诘诙诚诛诜话诞诟诠诡询诣诤该详诧诨诩诫诬语诮误诰诱诲诳说诵请诸诹诺读诼诽课诿谀谁谂调谄谅谆谇谈谊谋谌谍谎谏谐谑谒谓谔谕谖谗谙谚谛谜谝谟谠谡谢谣谤谥谦谧谨谩谪谬谭谮谯谰谱谲谳谴谵谶谷豁豆豇豉豌豕豚象豢豨豪豫豳豸豹豺貂貅貉貊貌貔貘贝贞负贡财责贤败账货质贩贪贫贬购贮贯贰贱贲贳贴贵贶贷贸费贺贻贼贽贾贿赁赂赃资赅赇赈赉赊赋赌赍赎赏赐赓赔赕赖赘赙赚赛赜赝赞赟赠赡赢赣赤赦赧赫赭走赳赴赵赶起趁趄超越趋趑趔趟趣趱足趴趵趸趺趾趿跃跄跆跋跌跎跏跐跑跖跗跚跛距跞跟跣跤跨跪跬路跳践跶跷跸跹跺跻踅踉踊踌踏踒踔踝踞踟踢踩踪踬踮踯踱踵踹踺踽蹀蹁蹂蹄蹇蹈蹉蹊蹋蹑蹒蹙蹚蹦蹩蹬蹭蹰蹲蹴蹶蹼蹽蹾蹿躁躅躇躏躐躜躞身躬躯躲躺车轧轨轩轫转轭轮软轰轱轲轳轴轶轸轻轼载轾轿辂较辄辅辆辇辈辉辊辋辍辎辏辐辑输辔辕辖辗辘辙辚辛辜辞辟辣辨辩辫辰辱边辽达迁迂迄迅过迈迎运近迓返迕还这进远违连迟迢迤迥迦迨迩迪迫迭迮述迷迸迹追退送适逃逄逅逆选逊逋逍透逐逑递途逖逗通逛逝逞速造逡逢逦逮逯逵逶逸逻逼逾遁遂遄遇遍遏遐遑遒道遗遛遢遣遥遨遭遮遴遵遽避邀邂邃邈邋邑邓邕邗邙邛邝邢那邦邪邬邮邯邰邱邳邴邵邶邸邹邺邻邾郁郄郅郇郊郎郏郑郓郗郛郜郝郡郢郤郦郧部郫郭郯郴郸都郾郿鄂鄄鄙鄞鄢鄯鄱酃酆酉酊酋酌配酐酒酗酚酝酞酡酢酣酤酥酩酪酬酮酯酰酱酵酶酷酸酹酽酿醅醇醉醋醌醍醐醒醚醛醢醪醮醯醴醺采釉释里重野量金釜鉴銮鋆鋈錾鍪鎏鏊鏖鐾鑫钇针钉钊钋钌钍钎钏钒钓钕钗钙钚钛钝钞钟钠钡钢钣钤钥钦钧钨钩钫钮钯钰钱钲钳钴钵钹钺钻钼钽钾钿铀铁铂铃铄铅铆铈铉铊铋铌铍铎铐铑铒铗铙铛铜铝铟铠铡铢铣铤铧铨铩铪铫铬铭铮铯铰铱铲铳铵银铷铸铺铻铼链铿销锁锂锃锄锅锆锇锈锉锋锌锏锐锑锒锔锕锗错锚锛锜锝锞锟锡锢锣锤锥锦锨锩锭键锯锰锱锲锴锵锶锷锸锹锻镀镁镂镇镉镊镌镍镏镐镑镒镓镔镖镗镘镚镛镜镝镞镠镡镢镣镦镧镩镪镫镬镭镯镰镲镳镴镵镶长门闩闪闭问闯闰闱闲闳间闵闷闸闹闺闻闼闽闾闿阀阁阂阃阄阅阆阇阈阉阊阋阍阎阏阐阑阒阔阕阖阗阙阚阜队阡阪阮阱防阳阴阵阶阻阿陀陂附际陆陇陈陉陋陌降限陔陕陛陟陡院除陨险陪陬陲陵陶陷隅隆隈隋隍随隐隔隗隘隙障隧隰隳隶隼隽难雀雁雄雅集雇雉雌雍雎雏雒雕雠雨雩雪雯雱雳零雷雹雾需霁霄霆震霈霉霍霎霏霓霖霜霞霨霪霭霰露霸霹霾青靓靖静靛非靠靡面靥革靰靳靴靶靸靺靼靿鞅鞋鞍鞑鞒鞘鞠鞡鞣鞧鞨鞫鞬鞭鞯鞲鞴韦韧韩韪韫韬韭音韵韶页顶顷顸项顺须顼顽顾顿颀颁颂颃预颅领颇颈颉颊颌颍颎颏颐频颓颔颖颗题颙颚颛颜额颞颟颠颡颢颤颦颧风飑飒飓飕飘飙飞食飧飨餍餐餮饔饕饥饧饨饩饪饫饬饭饮饯饰饱饲饴饵饶饷饸饹饺饼饽饿馁馃馄馅馆馇馈馊馋馍馏馐馑馒馓馔馕首馗香馥馨马驭驮驯驰驱驳驴驶驷驸驹驻驼驽驾驿骀骁骂骄骅骆骇骈骊骋验骎骏骐骑骒骓骖骗骘骚骛骜骝骞骟骠骡骢骣骤骥骧骨骰骶骷骸骺骼髀髁髂髅髋髌髑髓高髡髦髫髭髯髹髻鬃鬈鬏鬓鬟鬣鬲鬻鬼魁魂魃魄魅魆魇魈魉魍魏魑魔鱼鱿鲀鲁鲂鲅鲆鲇鲈鲋鲍鲎鲐鲑鲔鲚鲛鲜鲞鲟鲠鲡鲢鲣鲤鲥鲧鲨鲩鲫鲮鲱鲲鲳鲴鲵鲷鲸鲻鲼鲽鳀鳃鳄鳅鳇鳊鳌鳍鳎鳏鳐鳓鳔鳕鳖鳗鳙鳜鳝鳞鳟鳢鸟鸠鸡鸢鸣鸥鸦鸨鸩鸪鸫鸬鸭鸮鸯鸱鸲鸳鸵鸶鸷鸸鸹鸻鸽鸾鸿鹁鹂鹃鹄鹅鹆鹇鹈鹉鹊鹋鹌鹏鹐鹑鹕鹗鹘鹚鹛鹜鹞鹤鹦鹧鹩鹪鹫鹬鹭鹮鹰鹳鹿麂麇麈麋麒麓麝麟麦麸麻麽麾黄黍黎黏黑黔默黛黜黝黟黠黢黥黧黩黪黯黻黼黾鼋鼍鼎鼐鼓鼙鼠鼢鼬鼯鼹鼻鼾齁齉齐齑齿龀龃龄龅龇龈龉龊龋龌龙龚龛龟龠！（），：？𠳐𥻗𬉼가각간갇갈감갑값갓갔강갖같갚갛개객갤갱걀걔거걱건걷걸검겁것겉게겟겠겨격겪견결겹겼경곁계고곡곤곧골곰곱곳공과곽관광괜괴굉교구국군굳굴굵굶굽궁권궤귀귓규균귤그극근글긁금급긋긍기긴길김깃깅깊까깍깎깐깔깜깝깡깥깨꺅꺼꺾껌껍껏께껴꼬꼭꼴꼼꼽꽂꽃꽉꽝꽤꾀꾸꾼꿀꿈뀌끄끈끊끌끓끔끗끝끼낄낌나낙낚난날낡남납낫났낭낮낯낱낳내낼냄냅냇냈냉냐냥너넉넌널넓넘넣네넥넷녀녁년념녕녘노녹논놀놈농높놓놔뇌뇨누눈눌눕뉘뉴늄느늑는늘늙능늦늬니닉닌닐님닙닛닝다닥닦단닫달닭닮담답닷당닿대댁댐댓더덕던덜덟덤덥덧덩덮데델도독돈돌돔돕돗동돛돼됐되된될됨됩두둑둔둘둠둡둥뒤뒷듀듈드득든듣들듬듭듯등디딕딜딧딨딩딪따딸땀땅때떠떡떤떨떱떴떻떼또똑뚝뚫뚱뛰뜨뜩뜹뜻라락란람랍랑랗래랙랜램랫략량러럭런럴럼럽럿렁렇레렉렌렘렛려력련렬렴렵렸령례로록론롤롬롭롯뢰료루룩룹룻뤄류륙률륨륭르른를름릅릇릎리릭린릴림립릿링마막만많말맑맘맙맛망맞맡맣매맥맨맬맵맹맺머먹먼멀멈멋멍멎메멘며면멸명몇모목몬몰몸몹못몽묘무묵묶문묻물뭄뭇뭐뭔뭘뭣뮤므미민믿밀밉밌및밑바박밖반받발밝밟밤밥밧방밭배백밴밸뱀뱃뱅뱉버번벌범법벗베벤벨벼벽변별볍병볕보복볶본볼봄봅봇봉봐뵈뵙부북분불붉붐붑붓붕붙뷰브븐블비빅빈빌빔빕빗빙빚빛빠빡빨빵빼뺏뺨뻐뻔뻗뼈뼉뽑뾰뿌뿐뿔뿜쁘쁨사삭산살삶삼삿상새색샌샘생샤샵샷서석섞선설섬섭섯성세섹센셀셈셉셋셔션셨소속손솔솜솟송솥쇄쇠쇼수숙순숟술숨숫숭숲쉘쉬쉰쉽슈슐스슨슬슴습슷승시식신싣실싫심십싯싱싶싸싹싼쌀쌍쌓써썩썰썹쎄쏘쏟쏩쏴쑤쑥쓰쓴쓸씀씌씨씩씬씹씻아악안앉않알앓암압앗았앙앞애액앤앨앱앵야약얀얄얇얌양얕얗얘어억언얹얻얼엄업없엇었엉엊엌엎에엑엔엘여역연열엷염엽엿였영옆예옛오옥온올옮옳옵옷옹와완왓왔왕왜왠외왼요욕용우욱운울움웃웅워원월웠웨웬웰웸위윈윌윗유육윤율으윽은을음응의이익인일읽잃임입잇있잉잊잎자작잔잖잘잠잡잣장잦재잿쟁쟤저적전절젊점접젓정젖제젠젤젯져졌조족존졸좀좁종좋좌죄주죽준줄줌줍중줘쥐즈즉즌즐즘증지직진질짐집짓징짙짚짜짝짧째쨌쩌쩍쩐쩔쩜쪽쫓쭈쭉쯤찌찍찢차착찬찮찰참찻찼창찾채책챈챌챔챙처척천철첨첩첫청체쳐쳤초촉촌촛총촬최추축춘출춤춥춧충춰취츠측츰층치칙친칠침칫칭카칸칼캄캐캠캡커컨컬컴컵컷케켄켓켜켰코콘콜콤콩쾌쿄쿠쿰쿼퀄퀘퀴퀵큐크큰클큼큽키킬킵킷타탁탄탈탐탑탓탕태택탠탤탬탭탱터턱턴털텀텅테텍텐텔템토톤톨톰톱통퇴투툴툼퉁튀튕튜트특튼튿틀틈티틱틸팀팅파팎판팔팝패팩팬퍼퍽펀펄펑페펜펠펴편펼평폐포폭폰폴폼표푸푹풀품풍퓨프플픔피픽핀필핏핑하학한할함합항해핵핸햄햇했행향허헌험헤헬혀혁현혈협혔형혜호혹혼홀홈홉홍화확환활황회획횟횡효후훈훌훔훨휘휴흉흐흑흔흘흙흡흥흩희흰히힌힘힙";
	public static final String ENTITY_ALPHABET = " qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890-=+_!?,.&()";
	public static final String TAGS_ALPHABET = " qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890-=+_!?,.&$%@()";
	public static final String MAP_ALPHABET = " qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890-=+_!?,.&()'";
	public static final int MAX_NAME_LENGTH = 50;
	public static final int MAX_CHAT_LENGTH = 500;
	
	public static String makeFileSafe(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			String c = s.substring(i, i + 1);
			if (FILE_SAFE_ALPHABET.contains(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	public static boolean isMapSafe(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (!MAP_ALPHABET.contains(s.substring(i, i + 1))) { return false; }
		}
		return true;
	}
	
	public static Fount DEBUG_FOUNT = GUISetting.getFount("libmono12", "libmono12.txt");
	
	public static Fount FOUNT = GUISetting.getFount("libmono12", "libmono12.txt");
	public static Fount[] SMALL_FOUNT = { FOUNT };
	public static Fount[] MEDIUM_FOUNT = { FOUNT };
	public static Fount[] LARGE_FOUNT = { FOUNT };
	
	public static Fount FOUNT_OUTLINE = GUISetting.getFount("fatlibmono12", "fatlibmono12.txt");
	public static Fount[] SMALL_FOUNT_OUTLINE = { FOUNT_OUTLINE };
	public static Fount[] MEDIUM_FOUNT_OUTLINE = { FOUNT_OUTLINE };
	public static Fount[] LARGE_FOUNT_OUTLINE = { FOUNT_OUTLINE };
	
	public static Fount BIG_FOUNT_CONSTANT = GUISetting.getFount("fledermaus18", "fledermaus18.txt");
	public static Fount BIG_FOUNT = GUISetting.getFount("fledermaus18", "fledermaus18.txt");
	public static Fount[] SMALL_BIG_FOUNT = { BIG_FOUNT };
	public static Fount[] MEDIUM_BIG_FOUNT = { BIG_FOUNT };
	public static Fount[] LARGE_BIG_FOUNT = { BIG_FOUNT };
	
	public static Fount BIG_FOUNT_OUTLINE = GUISetting.getFount("fatfledermaus18", "fatfledermaus18.txt");
	public static Fount[] SMALL_BIG_FOUNT_OUTLINE = { BIG_FOUNT_OUTLINE };
	public static Fount[] MEDIUM_BIG_FOUNT_OUTLINE = { BIG_FOUNT_OUTLINE };
	public static Fount[] LARGE_BIG_FOUNT_OUTLINE = { BIG_FOUNT_OUTLINE };

	public static Fount BIGGER_FOUNT = GUISetting.getFount("fledermaus36", "fledermaus36.txt");
	public static Fount[] SMALL_BIGGER_FOUNT = { BIGGER_FOUNT };
	public static Fount[] MEDIUM_BIGGER_FOUNT = { BIGGER_FOUNT };
	public static Fount[] LARGE_BIGGER_FOUNT = { BIGGER_FOUNT };
	
	public static Fount MAP = GUISetting.getFount("libmono20", "libmono20.txt");
	public static Fount[] SMALL_MAP = { MAP };
	public static Fount[] MEDIUM_MAP = { MAP };
	public static Fount[] LARGE_MAP = { MAP };
	
	public static Fount MAP_OUTLINE = GUISetting.getFount("fatlibmono20", "fatlibmono20.txt");
	public static Fount[] SMALL_MAP_OUTLINE = { MAP_OUTLINE };
	public static Fount[] MEDIUM_MAP_OUTLINE = { MAP_OUTLINE };
	public static Fount[] LARGE_MAP_OUTLINE = { MAP_OUTLINE };
	
	public static Fount MAP_SMALL = GUISetting.getFount("libmono12", "libmono12.txt");
	public static Fount[] SMALL_MAP_SMALL = { MAP_SMALL };
	public static Fount[] MEDIUM_MAP_SMALL = { MAP_SMALL };
	public static Fount[] LARGE_MAP_SMALL = { MAP_SMALL };
	
	public static Fount MAP_SMALL_OUTLINE = GUISetting.getFount("fatlibmono12", "fatlibmono12.txt");
	public static Fount[] SMALL_MAP_SMALL_OUTLINE = { MAP_SMALL_OUTLINE };
	public static Fount[] MEDIUM_MAP_SMALL_OUTLINE = { MAP_SMALL_OUTLINE };
	public static Fount[] LARGE_MAP_SMALL_OUTLINE = { MAP_SMALL_OUTLINE };
	
	public static Fount MAP_FALLBACK = GUISetting.getFount("libmono20", "libmono20.txt");
	public static Fount[] SMALL_MAP_FALLBACK = { MAP_FALLBACK };
	public static Fount[] MEDIUM_MAP_FALLBACK = { MAP_FALLBACK };
	public static Fount[] LARGE_MAP_FALLBACK = { MAP_FALLBACK };
	
	public static Fount MAP_FALLBACK_OUTLINE = GUISetting.getFount("fatlibmono20", "fatlibmono20.txt");
	public static Fount[] SMALL_MAP_FALLBACK_OUTLINE = { MAP_FALLBACK_OUTLINE };
	public static Fount[] MEDIUM_MAP_FALLBACK_OUTLINE = { MAP_FALLBACK_OUTLINE };
	public static Fount[] LARGE_MAP_FALLBACK_OUTLINE = { MAP_FALLBACK_OUTLINE };
	
	public static Fount MAP_FALLBACK_SMALL = GUISetting.getFount("libmono12", "libmono12.txt");
	public static Fount[] SMALL_MAP_FALLBACK_SMALL = { MAP_FALLBACK_SMALL };
	public static Fount[] MEDIUM_MAP_FALLBACK_SMALL = { MAP_FALLBACK_SMALL };
	public static Fount[] LARGE_MAP_FALLBACK_SMALL = { MAP_FALLBACK_SMALL };
	
	public static Fount MAP_FALLBACK_SMALL_OUTLINE = GUISetting.getFount("fatlibmono12", "fatlibmono12.txt");
	public static Fount[] SMALL_MAP_FALLBACK_SMALL_OUTLINE = { MAP_FALLBACK_SMALL_OUTLINE };
	public static Fount[] MEDIUM_MAP_FALLBACK_SMALL_OUTLINE = { MAP_FALLBACK_SMALL_OUTLINE };
	public static Fount[] LARGE_MAP_FALLBACK_SMALL_OUTLINE = { MAP_FALLBACK_SMALL_OUTLINE };
	
	// Not (yet) scalable fonts.
	public static Fount HUGE_FOUNT = GUISetting.getFount("fledermaus64", "fledermaus64.txt");
	public static Fount[] HUGE_FOUNTS = { HUGE_FOUNT };
	public static Fount STENCIL = GUISetting.getFount("stencil", "stencil.txt");
	public static Fount[] STENCILS = { STENCIL };
	
	public static final int SGS = 16;
	public static final int PX_TO_M = 7;
	public static final int LIGHTMAP_DOWNSCALE = 4;
	
	public static final Clr HIGHLIGHT = new Clr(90, 255, 90, 200);
	public static final Clr DISCOURAGED = new Clr(255, 150, 100, 150);
	public static final Clr FORBIDDEN = new Clr(255, 90, 90);
	public static Clr SKY = Clr.fromHex("6aabed").mix(0.4, Clr.LIGHT_GREY);//new Clr(96, 150, 164);//Clr.fromHex("6aabed").mix(0.4, Clr.GREY);//new Clr(190, 195, 230);
	
	public static ArrayList<String> STRATEGIC_MUSIC = new ArrayList<String>(Arrays.asList("Suspensions", "Aloft", "Cumulonimbus", "Stratocumulus"));
	public static ArrayList<String> COMBAT_MUSIC = new ArrayList<String>(Arrays.asList("Xiuhtecuhtli", "Kartikeya", "Infernal Machines", "Ares", "Air and Fire", "Once More Into the Sky"));
	public static ArrayList<String> EDITOR_MUSIC = new ArrayList<String>(Arrays.asList("Stratospheres", "Shipyards"));
	public static ArrayList<String> CITY_MUSIC = new ArrayList<String>(Arrays.asList("Hesperus"));
	public static ArrayList<String> MENU_MUSIC = new ArrayList<String>();
	public static ArrayList<String> LOADING_MUSIC = new ArrayList<String>(Arrays.asList("Airships"));
	public static final ArrayList<String> NO_MUSIC = new ArrayList<String>();
	
	public static final String VERSION = "1.2.14";
	public static final int GROUND_LEVEL = 512;
	
	public static final boolean CHAT_OVERLAY_ENABLED = true;

	public static final int HEIGHT_FOR_DOUBLE_LIFT = 400;
	public static final double G = .001;
	public static final double LIFT_Y_EXPONENT = 1.5;
	public static final double LIFT_Y_DIVIDER = 30;

	public static final int SERVER_TICK = 64;
	
	public static final GuardedRandom ANIM_R = new GuardedRandom();
	
	public static boolean TICK_RENDER_LOCK = false;
	
	public static String wallTime() {
		Date d = new Date();
		return d.getHours() + ":" + d.getMinutes();
	}
	
	public static String[] normalizeTags(String[] tags) {
		ArrayList<String> ts = new ArrayList<String>();
		for (String t : tags) {
			if (!t.trim().isEmpty()) {
				ts.add(normalizeTag(t));
			}
		}
		return ts.toArray(new String[ts.size()]);
	}
	
	public static String normalizeTag(String tag) {
		tag = tag.trim();
		String[] words = tag.split(" ");
		tag = "";
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			tag += word.substring(0, 1).toUpperCase(Locale.ENGLISH) + word.substring(1).toLowerCase(Locale.ENGLISH);
			if (i != words.length - 1) {
				tag += " ";
			}
		}
		return tag;
	}
	
	public static boolean doWritechecksum() {
		return System.getProperty("writechecksum", "false").equals("true");
	}
	
	public static boolean isDebug() {
		return System.getProperty("debug", "false").equals("true");
	}
	
	public static boolean randomDataDir() {
		return System.getProperty("randomdatadir", "false").equals("true");
	}
	
	public static boolean isResumeDebug() {
		return System.getProperty("resumedebug", "false").equals("true");
	}
	
	public static boolean isRunTests() {
		return System.getProperty("runTests", "false").equals("true");
	}
	
	public static boolean useAttractMode() {
		return System.getProperty("attract", "false").equals("true");
	}
	
	public static boolean isDemo() {
		return System.getProperty("demo", "false").equals("true");
	}
	
	public static boolean debugCursor() {
		return System.getProperty("debugCursor", "false").equals("true");
	}
	
	public static void deleteDir(File dir) {
		if (dir.isDirectory()) {
			try {
				FileUtils.deleteDirectory(dir);
			} catch (IOException e) {
				doDeleteDir(dir);
			}
		} else if (dir.exists()) {
			dir.delete();
		}
	}
	
	private static void doDeleteDir(File dir) {
		if (dir.exists() && dir.isDirectory()) {
			for (File f : dir.listFiles()) {
				doDeleteDir(f);
			}
		}
		dir.delete();
	}
	
	public static void copyStatic(String srcName, String dstName) throws IOException {
		File src = new File(getStaticGameDirectory(), srcName);
		File dst = new File(getGameDirectory(), dstName);
		if (!dst.exists()) {
			FileUtils.copyDirectory(src, dst);
		}
	}
	
	public static void overlayStatic(String srcName, String dstName) throws IOException {
		File src = new File(getStaticGameDirectory(), srcName);
		File dst = new File(getGameDirectory(), dstName);
		if (!dst.exists()) {
			FileUtils.copyDirectory(src, dst);
		} else {
			File[] fs = src.listFiles();
			if (fs != null) {
				for (File f : fs) {
					File f2 = new File(dst, f.getName());
					if (!f2.exists()) {
						FileUtils.copyFile(f, f2);
					}
				}
			}
		}
	}

	public static File getStaticGameDirectory() {
		// 发布包启动脚本通过 -Dacs.staticdir 显式指定资源根目录（跨平台一致，macOS 亦适用）
		String override = System.getProperty("acs.staticdir");
		if (override != null && !override.isEmpty()) {
			return new File(override).getAbsoluteFile();
		}
		if (System.getProperty("dev", "false").equals("true")) {
			return new File("").getAbsoluteFile();
		}
		if (System.getProperty("os.name").contains("Mac")) {
			try {
				String path = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
				return new File(new File(path).getParentFile().getParentFile(), "Resources");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			String path = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			return new File(path).getParentFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new File("").getAbsoluteFile();
	}
	
	public static String getStaticGameDirectoryPath(String path) {
		File f = getStaticGameDirectory();
		for (String s : path.split("/")) {
			f = new File(f, s);
		}
		return f.getAbsolutePath();
	}
	
	private static boolean isFirstLogFileAccess = true;
	
	public static File getLogFile() {
		File logF = new File(getGameDirectory(), "log.txt");
		if (logF.exists() && isFirstLogFileAccess && FileUtils.sizeOf(logF) > 1024 * 1024) {
			try {
				FileUtils.moveFile(logF, new File(getGameDirectory(), "log-old-" + System.currentTimeMillis() + ".txt"));
			} catch (IOException e) {
				// Ignore
			}
		}
		if (isFirstLogFileAccess) {
			for (File f : getGameDirectory().listFiles()) {
				if (!f.isDirectory() && f.getName().startsWith("log-old-") && FileUtils.isFileOlder(f, System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30)) { // 30 days
					System.out.println("Deleting old log " + f.getAbsolutePath());
					FileUtils.deleteQuietly(f);
				}
			}
		}
		isFirstLogFileAccess = false;
		return logF;
	}
	
	public static File getGameDirectory() {
		if (LaunchSettings.customDataDirectoryLocation != null) {
			File f = new File(LaunchSettings.customDataDirectoryLocation);
			if (f.exists()) {
				if (f.isDirectory()) {
					return f;
				}
			} else {
				if (f.mkdirs()) {
					return f;
				}
			}
		}
		String appdata = System.getenv("APPDATA");
		if (appdata != null) {
			File dir = new File(new File(appdata), "AirshipsGame").getAbsoluteFile();
			dir.mkdirs();
			if (dir.exists()) {
				return dir;
			}
		}
		String home = System.getProperty("user.home");
		if (home != null) {
			if (System.getProperty("os.name").contains("Mac")) {
				File dir = new File(new File(new File(home), "Documents"), "AirshipsGame").getAbsoluteFile();
				dir.mkdirs();
				if (dir.exists()) {
					return dir;
				}
			} else {
				File dir = new File(new File(home), ".airshipsgame").getAbsoluteFile();
				dir.mkdirs();
				if (dir.exists()) {
					return dir;
				}
			}
		}
		File dir = new File("userdata");
		dir.mkdirs();
		return dir;
	}
	
	public static void resetGameData() {
		File gameDir = getGameDirectory();
		File instanceDir = new File(gameDir, "player " + System.currentTimeMillis());
		instanceDir.mkdirs();
		for (String name : new String[] { "buildings", "combats", "saves", "landships", "ships" }) {
			new File(gameDir, name).renameTo(new File(instanceDir, name));
		}
		for (String name : new String[] { "buildings", "landships", "ships"}) {
			try {
				AGame.copyStatic("default_" + name, name);
			} catch (IOException e) {
				AirshipGame.instance.reportError("Unabale to copy statics", e, name, true);
			}
		}
	}
	
	public static String getClipboardString() {
		try {
			return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static String getCodeFromClipboard() {
		String cs = getClipboardString();
		if (cs == null || !cs.trim().matches("^[a-zA-Z0-9]{4}(-[a-zA-Z0-9]{4})*$")) { return null; }
		return cs.trim();
	}
	
	/*
	static ArrayList<String> shipNames;
	static Locale shipNamesLocale;
	
	public static String getShipName(Locale l, GuardedRandom r) {
		if (shipNames == null || !l.equals(shipNamesLocale)) {
			File f = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "lang"), l.toLanguageTag() + "_airship_names.txt");
			try {
				shipNames = new ArrayList<String>(FileUtils.readLines(f, "UTF-8"));
			} catch (Exception e) {
				AirshipGame.instance.reportError("Unable to load building names for " + l, e, "", false, true);
				e.printStackTrace();
			}
			shipNamesLocale = l;
		}
		if (shipNames == null) {
			return "Airship " + r.nextInt(10000);
		}
		return shipNames.get(r.nextInt(shipNames.size()));
	}
	
	static ArrayList<String> buildingNames;
	static Locale buildingNamesLocale;
	
	public static String getBuildingName(Locale l, GuardedRandom r) {
		if (buildingNames == null || !l.equals(buildingNamesLocale)) {
			File f = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "lang"), l.toLanguageTag() + "_building_names.txt");
			try {
				buildingNames = new ArrayList<String>(FileUtils.readLines(f, "UTF-8"));
			} catch (Exception e) {
				AirshipGame.instance.reportError("Unable to load building names for " + l, e, "", false, true);
				e.printStackTrace();
			}
			buildingNamesLocale = l;
		}
		if (buildingNames == null) {
			return "Building " + r.nextInt(10000);
		}
		return buildingNames.get(r.nextInt(buildingNames.size()));
	}
	
	static ArrayList<String> aiShipNames;
	static Locale aiShipNamesLocale;
	
	public static String getAIShipName(Locale l, GuardedRandom r) {
		if (aiShipNames == null || !l.equals(aiShipNamesLocale)) {
			File f = new File(new File(new File(AGame.getStaticGameDirectory(), "data"), "lang"), l.toLanguageTag() + "_ai_airship_names.txt");
			try {
				aiShipNames = new ArrayList<String>(FileUtils.readLines(f, "UTF-8"));
			} catch (Exception e) {
				AirshipGame.instance.reportError("Unable to load ship names for " + l, e, "", false, true);
				e.printStackTrace();
			}
			aiShipNamesLocale = l;
		}
		if (aiShipNames == null) {
			return "Airship " + r.nextInt(10000);
		}
		return aiShipNames.get(r.nextInt(aiShipNames.size()));
	}
	*/
	
	public static double rnd(double min, double range, double extremeP, double extremeMin, double extremeRange) {
		if (AGame.ANIM_R.nextDouble() < extremeP) {
			return extremeMin + AGame.ANIM_R.nextDouble() * extremeRange;
		} else {
			return min + AGame.ANIM_R.nextDouble() * range;
		}
	}
	
	public static long cleanSeed(long seed) {
		return StrictMath.abs(seed) % 1000000;
	}
	
	public static long getSeed(Random r) {
		try {
			Field field = Random.class.getDeclaredField("seed");
			field.setAccessible(true);
			AtomicLong scrambledSeed = (AtomicLong) field.get(r);
			return scrambledSeed.get() ^ 0x5DEECE66DL;
		} catch (Exception e) {
			e.printStackTrace();
			return 12345;
		}
	}
	
	public static long getSeed(GuardedRandom r) {
		return getSeed(r.r);
	}
	
	public static boolean checkRandomSeedExtractionWorks() {
		Random r = new Random();
		Random r2 = new Random(getSeed(r));
		if (r.nextLong() != r2.nextLong()) { return false; }
		r2 = new Random(getSeed(r));
		return r.nextLong() == r2.nextLong();
	}
	
	public static <T> boolean containsAny(Collection<T> a, Collection<T> b) {
		for (T t : a) {
			if (b.contains(t)) { return true; }
		}
		return false;
	}
}
