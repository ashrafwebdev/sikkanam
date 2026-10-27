package com.ashrafwebdev.sikkanam.learn

/**
 * One item to learn. Every item has the original Arabic, a transliteration in Tamil and in English,
 * and the meaning in Tamil and English. Entries with no meaning (for example, a formula) leave the
 * meaning blank.
 *
 * NOTE: The Tamil transliterations and Tamil meanings are unreviewed. Have a qualified teacher check
 * all content before it is used for teaching.
 */
data class LearnItem(
    val id: String,
    val title: String,
    val arabic: String,
    val tamilTransliteration: String,
    val englishTransliteration: String,
    val meaningTamil: String,
    val meaningEnglish: String,
)

data class LearnCategory(val title: String, val items: List<LearnItem>)

object LearnContent {
    val categories: List<LearnCategory> = listOf(
        LearnCategory(
            title = "Surahs (learn at least ten)",
            items = listOf(
                LearnItem(
                    id = "fatiha", title = "Surah Al-Fatiha (1)",
                    arabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۝ الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ ۝ الرَّحْمَٰنِ الرَّحِيمِ ۝ مَالِكِ يَوْمِ الدِّينِ ۝ إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ ۝ اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ ۝ صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ",
                    tamilTransliteration = "பிஸ்மில்லாஹிர் ரஹ்மானிர் ரஹீம். அல்ஹம்து லில்லாஹி ரப்பில் ஆலமீன். அர்ரஹ்மானிர் ரஹீம். மாலிகி யவ்மித் தீன். இய்யாக்க நஃபுது வ இய்யாக்க நஸ்தஈன். இஹ்தினஸ் சிராதல் முஸ்தகீம். சிராதல் லதீன அன்அம்த்த அலைஹிம், கைரில் மக்லூபி அலைஹிம் வலத் தாலீன்.",
                    englishTransliteration = "Bismillahir-Rahmanir-Raheem. Alhamdu lillahi Rabbil-'alameen. Ar-Rahmanir-Raheem. Maaliki yawmid-deen. Iyyaaka na'budu wa iyyaaka nasta'een. Ihdinas-siraatal-mustaqeem. Siraatal-ladheena an'amta 'alayhim, ghayril-maghdoobi 'alayhim wa lad-daalleen.",
                    meaningTamil = "அளவற்ற அருளாளனும் நிகரற்ற அன்புடையோனுமாகிய அல்லாஹ்வின் பெயரால். அகிலங்களின் இறைவனாகிய அல்லாஹ்வுக்கே எல்லாப் புகழும். அவன் அளவற்ற அருளாளன், நிகரற்ற அன்புடையோன். தீர்ப்பு நாளின் அதிபதி. உன்னையே நாங்கள் வணங்குகிறோம்; உன்னிடமே உதவி தேடுகிறோம். எங்களை நேரான வழியில் செலுத்து. நீ அருள் புரிந்தவர்களின் வழியில்; கோபத்திற்கு ஆளானவர்களின் வழியிலும், வழி தவறியவர்களின் வழியிலும் அல்ல.",
                    meaningEnglish = "In the name of Allah, the Most Gracious, the Most Merciful. All praise is for Allah, Lord of the worlds. The Most Gracious, the Most Merciful. Master of the Day of Judgement. You alone we worship, and You alone we ask for help. Guide us to the straight path, the path of those You have blessed, not of those who earned anger, nor of those who went astray.",
                ),
                LearnItem(
                    id = "ikhlas", title = "Surah Al-Ikhlas (112)",
                    arabic = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ",
                    tamilTransliteration = "குல் ஹுவல்லாஹு அஹத். அல்லாஹுஸ் ஸமத். லம் யலித், வ லம் யூலத். வ லம் யகுல்லஹு குஃபுவன் அஹத்.",
                    englishTransliteration = "Qul huwa Allahu ahad. Allahus-samad. Lam yalid wa lam yoolad. Wa lam yakullahu kufuwan ahad.",
                    meaningTamil = "நீர் கூறுவீராக: அவன் அல்லாஹ், ஒரே இறைவன். அல்லாஹ் எவரிடமும் தேவையற்றவன். அவன் எவரையும் பெறவில்லை; அவன் எவராலும் பெறப்படவில்லை. அவனுக்கு நிகராக எவரும் இல்லை.",
                    meaningEnglish = "Say: He is Allah, the One. Allah, the Eternal, the Self-Sufficient. He neither begets nor is born. And there is none comparable to Him.",
                ),
                LearnItem(
                    id = "falaq", title = "Surah Al-Falaq (113)",
                    arabic = "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِن شَرِّ مَا خَلَقَ ۝ وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ",
                    tamilTransliteration = "குல் அஊது பி ரப்பில் ஃபலக். மின் ஷர்ரி மா கலக். வ மின் ஷர்ரி ஙாஸிகின் இதா வகப். வ மின் ஷர்ரின் நப்பாதாதி ஃபில் உகத். வ மின் ஷர்ரி ஹாஸிதின் இதா ஹஸத்.",
                    englishTransliteration = "Qul a'oodhu bi Rabbil-falaq. Min sharri ma khalaq. Wa min sharri ghaasiqin idha waqab. Wa min sharrin-naffaathaati fil-'uqad. Wa min sharri haasidin idha hasad.",
                    meaningTamil = "நீர் கூறுவீராக: விடியலின் இறைவனிடம் நான் பாதுகாவல் தேடுகிறேன். அவன் படைத்தவற்றின் தீமையிலிருந்தும், இருள் சூழும்போது ஏற்படும் தீமையிலிருந்தும், முடிச்சுகளில் ஊதும் (சூனியக்காரிகளின்) தீமையிலிருந்தும், பொறாமைக்காரன் பொறாமைப்படும்போது அவனுடைய தீமையிலிருந்தும்.",
                    meaningEnglish = "Say: I seek refuge with the Lord of the daybreak, from the evil of what He created, from the evil of darkness when it settles, from the evil of those who blow on knots, and from the evil of the envier when he envies.",
                ),
                LearnItem(
                    id = "nas", title = "Surah An-Nas (114)",
                    arabic = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ",
                    tamilTransliteration = "குல் அஊது பி ரப்பின் நாஸ். மலிகின் நாஸ். இலாஹின் நாஸ். மின் ஷர்ரில் வஸ்வாசில் கன்னாஸ். அல்லதீ யுவஸ்வ திஃபீ ஸுதூரின் நாஸ். மினல் ஜின்னத்தி வன் நாஸ்.",
                    englishTransliteration = "Qul a'oodhu bi Rabbin-naas. Malikin-naas. Ilahin-naas. Min sharril-waswaasil-khannaas. Alladhee yuwaswisu fee sudoorin-naas. Minal-jinnati wan-naas.",
                    meaningTamil = "நீர் கூறுவீராக: மனிதர்களின் இறைவனிடம் நான் பாதுகாவல் தேடுகிறேன். மனிதர்களின் அரசனிடம், மனிதர்களின் கடவுளிடம். மறைந்து மறைந்து கிசுகிசுக்கும் ஷைத்தானின் தீமையிலிருந்து. அவன் மனிதர்களின் உள்ளங்களில் கிசுகிசுக்கிறான்; அவன் ஜின்களிலும் மனிதர்களிலும் உள்ளவன்.",
                    meaningEnglish = "Say: I seek refuge with the Lord of mankind, the King of mankind, the God of mankind, from the evil of the whisperer who withdraws, who whispers into the hearts of mankind, from among jinn and mankind.",
                ),
                LearnItem(
                    id = "kawthar", title = "Surah Al-Kawthar (108)",
                    arabic = "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ ۝ فَصَلِّ لِرَبِّكَ وَانْحَرْ ۝ إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ",
                    tamilTransliteration = "இன்னா அஃதைனாகல் கவ்ஸர். ஃபஸல்லி லிரப்பிக்க வன்ஹர். இன்ன ஷானிஅக ஹுவல் அப்தர்.",
                    englishTransliteration = "Inna a'taynaakal-kawthar. Fa salli li Rabbika wanhar. Inna shaani'aka huwal-abtar.",
                    meaningTamil = "நிச்சயமாக நாம் உமக்கு கவ்ஸரை (சுவனத்தின் ஒரு நீர்த்தடாகம்) அளித்தோம். ஆகவே உமது இறைவனுக்காகத் தொழுது அறுத்துப் பலியிடுவீராக. நிச்சயமாக உம்மை வெறுப்பவனே வேரற்றவன்.",
                    meaningEnglish = "Indeed, We have granted you al-Kawthar. So pray to your Lord and sacrifice. Indeed, your enemy is the one cut off.",
                ),
                LearnItem(
                    id = "nasr", title = "Surah An-Nasr (110)",
                    arabic = "إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ ۝ وَرَأَيْتَ النَّاسَ يَدْخُلُونَ فِي دِينِ اللَّهِ أَفْوَاجًا ۝ فَسَبِّحْ بِحَمْدِ رَبِّكَ وَاسْتَغْفِرْهُ ۚ إِنَّهُ كَانَ تَوَّابًا",
                    tamilTransliteration = "இதா ஜாஅ நஸ்ருல்லாஹி வல் ஃபத்ஹ். வ ரஅய்தன் நாஸ யத்குலூன ஃபீ தீனில்லாஹி அஃப்வாஜா. ஃபஸப்பிஹ் பிஹம்தி ரப்பிக்க வஸ்தஃக்ஃபிரஹ். இன்னஹு கான தவ்வாபா.",
                    englishTransliteration = "Idha jaa'a nasrullahi wal-fath. Wa ra'aytan-naasa yadkhuloona fee deenillahi afwaja. Fasabbih bihamdi Rabbika wastaghfirh. Innahu kaana tawwaba.",
                    meaningTamil = "அல்லாஹ்வின் உதவியும் வெற்றியும் வந்துவிட்டால், மக்கள் அல்லாஹ்வின் மார்க்கத்தில் கூட்டம் கூட்டமாக நுழைவதை நீர் காணும்போது, உமது இறைவனைப் புகழ்ந்து துதித்து, அவனிடம் மன்னிப்புக் கோருவீராக. நிச்சயமாக அவன் மன்னிப்பை ஏற்பவன்.",
                    meaningEnglish = "When the help of Allah and the victory have come, and you see people entering the religion of Allah in multitudes, then glorify your Lord with praise and seek His forgiveness. Indeed, He is ever accepting of repentance.",
                ),
                LearnItem(
                    id = "kafirun", title = "Surah Al-Kafirun (109)",
                    arabic = "قُلْ يَا أَيُّهَا الْكَافِرُونَ ۝ لَا أَعْبُدُ مَا تَعْبُدُونَ ۝ وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُدُ ۝ وَلَا أَنَا عَابِدٌ مَّا عَبَدتُّمْ ۝ وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُدُ ۝ لَكُمْ دِينُكُمْ وَلِيَ دِينِ",
                    tamilTransliteration = "குல்யா அய்யுஹல் காஃபிரூன். லா அஃபுது மா தஃபுதூன். வ லா அன்தும் ஆபிதூன மா அஃபுது. வ லா அன ஆபிதுன் மா அபத்தும். வ லா அன்தும் ஆபிதூன மா அஃபுத். லகும் தீனுகும் வலிய தீன்.",
                    englishTransliteration = "Qul ya ayyuhal-kafiroon. La a'budu ma ta'budoon. Wa la antum 'aabidoona ma a'bud. Wa la ana 'aabidun ma 'abadtum. Wa la antum 'aabidoona ma a'bud. Lakum deenukum wa liya deen.",
                    meaningTamil = "நீர் கூறுவீராக: நிராகரிப்பவர்களே! நீங்கள் வணங்குவதை நான் வணங்கமாட்டேன். நான் வணங்குவதை நீங்கள் வணங்குபவர்களும் அல்ல. நான் வணங்கியதை நீங்கள் வணங்கியவர்களாக இல்லை; நீங்கள் வணங்குவதை நான் வணங்குபவனாக இல்லை. உங்களுக்கு உங்கள் மார்க்கம்; எனக்கு என் மார்க்கம்.",
                    meaningEnglish = "Say: O disbelievers, I do not worship what you worship, nor do you worship what I worship. Nor will I worship what you have worshipped, nor will you worship what I worship. For you is your religion, and for me is my religion.",
                ),
                LearnItem(
                    id = "maun", title = "Surah Al-Ma'un (107)",
                    arabic = "أَرَأَيْتَ الَّذِي يُكَذِّبُ بِالدِّينِ ۝ فَذَٰلِكَ الَّذِي يَدُعُّ الْيَتِيمَ ۝ وَلَا يَحُضُّ عَلَىٰ طَعَامِ الْمِسْكِينِ ۝ فَوَيْلٌ لِّلْمُصَلِّينَ ۝ الَّذِينَ هُمْ عَن صَلَاتِهِمْ سَاهُونَ ۝ الَّذِينَ هُمْ يُرَاءُونَ ۝ وَيَمْنَعُونَ الْمَاعُونَ",
                    tamilTransliteration = "அரஅய்தல்லதீ யுகத்திபு பித்தீன். ஃபதாலிகல்லதீ யதுஉல் யதீம். வ லா யஹ்லுது அலா த்தஆமில் மிஸ்கீன். ஃபவைலுல் லில் முஸல்லீன். அல்லதீன ஹும் அன் ஸலாதிஹிம் ஸாஹூன். அல்லதீன ஹும் யுராஊன். வ யமனஊனல் மாஊன்.",
                    englishTransliteration = "Ara'aytal-ladhee yukadhdhibu bid-deen. Fathaalikal-ladhee yadu'ul-yateem. Wa laa yahuddu 'alaa ta'aamil-miskeen. Fawaylul-lil-musalleen. Alladheena hum 'an salaatihim saahoon. Alladheena hum yuraa'oon. Wa yamna'oonal-maa'oon.",
                    meaningTamil = "தீர்ப்பு நாளைப் பொய்ப்பிப்பவனை நீர் பார்த்தீரா? அவன்தான் அநாதையை விரட்டுபவன்; ஏழைக்கு உணவளிக்கத் தூண்டாதவன். ஆகவே தொழுபவர்களுக்குக் கேடு உண்டு; அவர்கள் தங்கள் தொழுகையில் அலட்சியமாக இருப்பவர்கள்; அவர்கள் (மக்களுக்குக் காட்டவே) செயல்படுபவர்கள்; பொருளுதவியைத் தடுப்பவர்கள்.",
                    meaningEnglish = "Have you seen the one who denies the Recompense? That is the one who drives away the orphan, and does not urge the feeding of the poor. So woe to those who pray, who are heedless of their prayer, who do good only to be seen, and who withhold small kindnesses.",
                ),
                LearnItem(
                    id = "quraysh", title = "Surah Quraysh (106)",
                    arabic = "لِإِيلَافِ قُرَيْشٍ ۝ إِيلَافِهِمْ رِحْلَةَ الشِّتَاءِ وَالصَّيْفِ ۝ فَلْيَعْبُدُوا رَبَّ هَٰذَا الْبَيْتِ ۝ الَّذِي أَطْعَمَهُم مِّن جُوعٍ وَآمَنَهُم مِّنْ خَوْفٍ",
                    tamilTransliteration = "லி ஈலாஃபி குறைஷின். ஈலாஃபிஹிம் ரிஹ்லதஷ் ஷிதாஇ வஸ் ஸைஃப். ஃபல்யஃபுது ரப்ப ஹாதல் பைத். அல்லதீ அத்அமஹும் மின் ஜூஇன் வ ஆமனஹும் மின் கவ்ஃப்.",
                    englishTransliteration = "Li-eelaafi Quraysh. Eelaafihim rihlatash-shitaa'i wa-sayf. Falya'budoo Rabba haadhal-Bayt. Alladhee at'amahum min joo'in wa aamanahum min khawf.",
                    meaningTamil = "குறைஷிகளின் பழக்கத்திற்காக, குளிர்காலம் மற்றும் கோடைகாலப் பயணங்களின் பழக்கத்திற்காக. ஆகவே இந்த இல்லத்தின் இறைவனை அவர்கள் வணங்கட்டும். அவன் பசியிலிருந்து அவர்களுக்கு உணவளித்தான்; பயத்திலிருந்து அவர்களுக்குப் பாதுகாப்பளித்தான்.",
                    meaningEnglish = "For the accustomed security of the Quraysh, their accustomed security in the caravan of winter and summer. So let them worship the Lord of this House, who has fed them against hunger and made them safe from fear.",
                ),
                LearnItem(
                    id = "fil", title = "Surah Al-Fil (105)",
                    arabic = "أَلَمْ تَرَ كَيْفَ فَعَلَ رَبُّكَ بِأَصْحَابِ الْفِيلِ ۝ أَلَمْ يَجْعَلْ كَيْدَهُمْ فِي تَضْلِيلٍ ۝ وَأَرْسَلَ عَلَيْهِمْ طَيْرًا أَبَابِيلَ ۝ تَرْمِيهِم بِحِجَارَةٍ مِّن سِجِّيلٍ ۝ فَجَعَلَهُمْ كَعَصْفٍ مَّأْكُولٍ",
                    tamilTransliteration = "அலம் தர கைஃப ஃபஅல ரப்புக்க பி அஸ்ஹாபில் ஃபீல். அலம் யஜ்அல் கய்தஹும் ஃபீ தள்லீல். வ அர்ஸல அலைஹிம் தையரன் அபாபீல். தர்மீஹிம் பி ஹிஜாரதின் மின் ஸிஜ்ஜீல். ஃபஜ அலஹும் கஅஸ்ஃபின் மஃகூல்.",
                    englishTransliteration = "Alam tara kayfa fa'ala Rabbuka bi-ashaabil-feel. Alam yaj'al kaydahum fee tadleel. Wa arsala 'alayhim tayran abaabeel. Tarmeehim bi-hijaaratin min sijjeel. Faja'alahum ka'asfin ma'kool.",
                    meaningTamil = "யானைப் படையினருக்கு உம் இறைவன் என்ன செய்தான் என்பதை நீர் பார்க்கவில்லையா? அவர்களுடைய சூழ்ச்சியை வீணாக்கவில்லையா? அவர்கள் மீது பறவைக் கூட்டங்களை அனுப்பினான். சிஜ்ஜீல் என்ற கல்லைக் கொண்டு அவற்றை எறிந்தன. அவர்களை மென்று தின்ற வைக்கோல் போல் ஆக்கினான்.",
                    meaningEnglish = "Have you not considered how your Lord dealt with the companions of the elephant? Did He not make their plan go astray? And He sent against them birds in flocks, striking them with stones of hard clay, and He made them like eaten straw.",
                ),
            ),
        ),
        LearnCategory(
            title = "Essential duas",
            items = listOf(
                LearnItem(
                    id = "dua_toilet_in", title = "Before entering the toilet",
                    arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ",
                    tamilTransliteration = "அல்லாஹும்ம இன்னீ அஊது பிக்க மினல் குபுதி வல் கபாஇஸ்.",
                    englishTransliteration = "Allahumma innee a'oodhu bika minal-khubuthi wal-khaba'ith.",
                    meaningTamil = "இறைவா! ஆண் ஜின்கள் மற்றும் பெண் ஜின்களின் தீமையிலிருந்து உன்னிடம் பாதுகாப்புத் தேடுகிறேன்.",
                    meaningEnglish = "O Allah, I seek refuge in You from the male and female evil spirits.",
                ),
                LearnItem(
                    id = "dua_eat", title = "Before eating",
                    arabic = "بِسْمِ اللَّهِ",
                    tamilTransliteration = "பிஸ்மில்லாஹ்.",
                    englishTransliteration = "Bismillah.",
                    meaningTamil = "அல்லாஹ்வின் பெயரால்.",
                    meaningEnglish = "In the name of Allah.",
                ),
                LearnItem(
                    id = "dua_eat_forgot", title = "If you forgot Bismillah at the start of eating",
                    arabic = "بِسْمِ اللَّهِ أَوَّلَهُ وَآخِرَهُ",
                    tamilTransliteration = "பிஸ்மில்லாஹி அவ்வலஹு வ ஆகிரஹ்.",
                    englishTransliteration = "Bismillahi awwalahu wa aakhirahu.",
                    meaningTamil = "அதன் ஆரம்பத்திலும் முடிவிலும் அல்லாஹ்வின் பெயரால்.",
                    meaningEnglish = "In the name of Allah, at its beginning and its end.",
                ),
                LearnItem(
                    id = "dua_eat_after", title = "After eating",
                    arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
                    tamilTransliteration = "அல்ஹம்து லில்லாஹில்லதீ அத்அமனீ ஹாதா வ ரஸக்கனீஹி மின் கைரி ஹவ்லின் மின்னீ வலா குவ்வா.",
                    englishTransliteration = "Alhamdu lillahil-ladhee at'amanee hadha wa razaqaneehi min ghayri hawlin minnee wa laa quwwa.",
                    meaningTamil = "இதை எனக்கு உணவளித்து, என் சக்தியோ பலமோ இன்றி எனக்கு வழங்கிய அல்லாஹ்வுக்கே எல்லாப் புகழும்.",
                    meaningEnglish = "All praise is for Allah, who fed me this and provided it for me without any strength or power from me.",
                ),
                LearnItem(
                    id = "dua_leave_toilet", title = "After leaving the toilet",
                    arabic = "غُفْرَانَكَ",
                    tamilTransliteration = "குஃப்ரானக.",
                    englishTransliteration = "Ghufraanak.",
                    meaningTamil = "(இறைவா!) உன்னிடம் மன்னிப்புக் கோருகிறேன்.",
                    meaningEnglish = "I seek Your forgiveness.",
                ),
                LearnItem(
                    id = "dua_sleep", title = "Before sleeping",
                    arabic = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
                    tamilTransliteration = "பிஸ்மிக்க அல்லாஹும்ம அமூது வ அஹ்யா.",
                    englishTransliteration = "Bismika Allahumma amootu wa ahyaa.",
                    meaningTamil = "அல்லாஹ்வே! உன் பெயரால் நான் இறக்கிறேன், வாழ்கிறேன்.",
                    meaningEnglish = "In Your name, O Allah, I die and I live.",
                ),
                LearnItem(
                    id = "dua_wake", title = "On waking up",
                    arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
                    tamilTransliteration = "அல்ஹம்து லில்லாஹில்லதீ அஹ்யானா பஃது மா அமாதனா வ இலைஹின் நுஷூர்.",
                    englishTransliteration = "Alhamdu lillahil-ladhee ahyaanaa ba'da maa amaatanaa wa ilayhin-nushoor.",
                    meaningTamil = "எங்களை மரணிக்கச் செய்த பின் எங்களை உயிர்ப்பித்த அல்லாஹ்வுக்கே எல்லாப் புகழும்; அவனிடமே மீண்டெழுதல்.",
                    meaningEnglish = "All praise is for Allah, who gave us life after He had caused us to die, and to Him is the resurrection.",
                ),
                LearnItem(
                    id = "dua_leave_home", title = "When leaving home",
                    arabic = "بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
                    tamilTransliteration = "பிஸ்மில்லாஹி தவக்கல்து அலல்லாஹி வலா ஹவ்ல வலா குவ்வத்த இல்லா பில்லாஹ்.",
                    englishTransliteration = "Bismillahi tawakkaltu 'alallahi wa laa hawla wa laa quwwata illaa billah.",
                    meaningTamil = "அல்லாஹ்வின் பெயரால். அல்லாஹ்வையே நம்பினேன். அல்லாஹ்வின் உதவியின்றி எந்த சக்தியும் பலமும் இல்லை.",
                    meaningEnglish = "In the name of Allah. I place my trust in Allah. There is no power and no strength except with Allah.",
                ),
                LearnItem(
                    id = "dua_travel", title = "Travel dua",
                    arabic = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنقَلِبُونَ",
                    tamilTransliteration = "சுப்ஹானல்லதீ சக்கர லனா ஹாதா வ மா குன்ன லஹு முக்ரினீன். வ இன்னா இலா ரப்பினா லமுன்கலிபூன்.",
                    englishTransliteration = "Subhaanal-ladhee sakhkhara lanaa haadhaa wa maa kunnaa lahu muqrineen. Wa innaa ilaa Rabbinaa lamunqaliboon.",
                    meaningTamil = "இதை எங்களுக்கு வசப்படுத்தித் தந்தவன் தூயவன். இதை எங்களால் வசப்படுத்த முடியாது. நிச்சயமாக நாங்கள் எங்கள் இறைவனிடமே திரும்பிச் செல்பவர்கள்.",
                    meaningEnglish = "Glory be to Him who has subjected this to us, and we could not have done it ourselves. And to our Lord we will surely return.",
                ),
                LearnItem(
                    id = "dua_istighfar", title = "Seeking forgiveness",
                    arabic = "أَسْتَغْفِرُ اللَّهَ",
                    tamilTransliteration = "அஸ்தக்ஃபிருல்லாஹ்.",
                    englishTransliteration = "Astaghfirullah.",
                    meaningTamil = "அல்லாஹ்விடம் மன்னிப்புக் கோருகிறேன்.",
                    meaningEnglish = "I seek forgiveness from Allah.",
                ),
            ),
        ),
        LearnCategory(
            title = "Adhan and iqama",
            items = listOf(
                LearnItem(
                    id = "adhan", title = "Adhan (call to prayer)",
                    arabic = "اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ ۝ اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ ۝ أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ ۝ أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ ۝ أَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ اللَّهِ ۝ أَشْهَدُ أَنَّ مُحَمَّدًا رَسُولُ اللَّهِ ۝ حَيَّ عَلَى الصَّلَاةِ ۝ حَيَّ عَلَى الصَّلَاةِ ۝ حَيَّ عَلَى الْفَلَاحِ ۝ حَيَّ عَلَى الْفَلَاحِ ۝ اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ ۝ لَا إِلَٰهَ إِلَّا اللَّهُ",
                    tamilTransliteration = "அல்லாஹு அக்பர் அல்லாஹு அக்பர். அல்லாஹு அக்பர் அல்லாஹு அக்பர். அஷ்ஹது அல்லா இலாஹ இல்லல்லாஹ். அஷ்ஹது அல்லா இலாஹ இல்லல்லாஹ். அஷ்ஹது அன்ன முஹம்மதர் ரசூலல்லாஹ். அஷ்ஹது அன்ன முஹம்மதர் ரசூலல்லாஹ். ஹய்ய அலஸ் ஸலாஹ். ஹய்ய அலஸ் ஸலாஹ். ஹய்ய அலல் ஃபலாஹ். ஹய்ய அலல் ஃபலாஹ். அல்லாஹு அக்பர் அல்லாஹு அக்பர். லா இலாஹ இல்லல்லாஹ்.",
                    englishTransliteration = "Allahu akbar, Allahu akbar. Allahu akbar, Allahu akbar. Ashhadu an laa ilaha illallah, Ashhadu an laa ilaha illallah. Ashhadu anna Muhammadan Rasoolullah, Ashhadu anna Muhammadan Rasoolullah. Hayya 'alas-salah, Hayya 'alas-salah. Hayya 'alal-falah, Hayya 'alal-falah. Allahu akbar, Allahu akbar. Laa ilaha illallah.",
                    meaningTamil = "அல்லாஹ் மிகப் பெரியவன். அல்லாஹ்வைத் தவிர வணக்கத்திற்குரியவன் இல்லை என்று சாட்சி கூறுகிறேன். முஹம்மத் (ஸல்) அல்லாஹ்வின் தூதர் என்று சாட்சி கூறுகிறேன். தொழுகைக்கு வாருங்கள். வெற்றிக்கு வாருங்கள். அல்லாஹ்வைத் தவிர வணக்கத்திற்குரியவன் இல்லை.",
                    meaningEnglish = "Allah is the Greatest. I bear witness that there is no god but Allah. I bear witness that Muhammad is the Messenger of Allah. Come to prayer. Come to success. Allah is the Greatest. There is no god but Allah.",
                ),
                LearnItem(
                    id = "adhan_fajr", title = "Fajr adhan addition",
                    arabic = "الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ ، الصَّلَاةُ خَيْرٌ مِنَ النَّوْمِ",
                    tamilTransliteration = "அஸ்ஸலாது கைருன் மினன் நவ்ம். அஸ்ஸலாது கைருன் மினன் நவ்ம்.",
                    englishTransliteration = "As-salaatu khayrun minan-nawm. As-salaatu khayrun minan-nawm.",
                    meaningTamil = "தூக்கத்தை விட தொழுகை சிறந்தது (இரண்டு முறை). இது ஃபஜ்ர் அதானில் ‘ஹய்ய அலல் ஃபலாஹ்’ க்குப் பிறகு சொல்லப்படுகிறது.",
                    meaningEnglish = "Prayer is better than sleep (said twice). This is said in the Fajr adhan after 'Hayya 'alal-falah'.",
                ),
                LearnItem(
                    id = "iqama", title = "Iqama (before the prayer starts)",
                    arabic = "قَدْ قَامَتِ الصَّلَاةُ قَدْ قَامَتِ الصَّلَاةُ ۝ اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ ۝ لَا إِلَٰهَ إِلَّا اللَّهُ",
                    tamilTransliteration = "கத் கா மத்திஸ் ஸலாஹ். கத் கா மத்திஸ் ஸலாஹ். அல்லாஹு அக்பர் அல்லாஹு அக்பர். லா இலாஹ இல்லல்லாஹ்.",
                    englishTransliteration = "Qad qaamatis-salah, Qad qaamatis-salah. Allahu akbar, Allahu akbar. Laa ilaha illallah.",
                    meaningTamil = "தொழுகை ஆரம்பமாகிவிட்டது (இரண்டு முறை). அல்லாஹ் மிகப் பெரியவன். அல்லாஹ்வைத் தவிர வணக்கத்திற்குரியவன் இல்லை.",
                    meaningEnglish = "The prayer has begun (twice). Allah is the Greatest. There is no god but Allah.",
                ),
            ),
        ),
        LearnCategory(
            title = "The five daily prayers",
            items = listOf(
                LearnItem(
                    id = "fajr", title = "Fajr: 2 fard rakats",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "ஃபஜ்ர் தொழுகை: 2 ஃபர்ள் ரக்அத். தொழுகைக்கு முன் 2 சுன்னத் ரக்அத் (உறுதியான சுன்னத்).",
                    meaningEnglish = "Fajr: 2 fard rakats. Two sunnah rakats are prayed before it.",
                ),
                LearnItem(
                    id = "zuhr", title = "Zuhr: 4 fard rakats",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "லுஹர் தொழுகை: 4 ஃபர்ள் ரக்அத். முன்னும் பின்னும் சுன்னத் ரக்அத்கள் உண்டு.",
                    meaningEnglish = "Zuhr: 4 fard rakats. Sunnah rakats are prayed before and after it.",
                ),
                LearnItem(
                    id = "asr", title = "Asr: 4 fard rakats",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "அஸர் தொழுகை: 4 ஃபர்ள் ரக்அத்.",
                    meaningEnglish = "Asr: 4 fard rakats.",
                ),
                LearnItem(
                    id = "maghrib", title = "Maghrib: 3 fard rakats",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "மஃக்ரிப் தொழுகை: 3 ஃபர்ள் ரக்அத். தொழுகைக்குப் பின் 2 சுன்னத் ரக்அத்.",
                    meaningEnglish = "Maghrib: 3 fard rakats. Two sunnah rakats follow it.",
                ),
                LearnItem(
                    id = "isha", title = "Isha: 4 fard rakats",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "இஷா தொழுகை: 4 ஃபர்ள் ரக்அத். பின்னர் வித்ர் (ஹனஃபி: 3 ரக்அத், வாஜிப்).",
                    meaningEnglish = "Isha: 4 fard rakats. Then Witr (Hanafi: 3 rakats, wajib).",
                ),
            ),
        ),
        LearnCategory(
            title = "How to lead the prayer (imam)",
            items = listOf(
                LearnItem(
                    id = "imam_lead", title = "Leading the five prayers",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "ஜமாஅத்தை வழிநடத்தும் இமாம்: முதலில் நிய்யத் செய்து, 'அல்லாஹு அக்பர்' என்று கூறி தக்பீர் தஹ்ரீமா செய்ய வேண்டும். பின்னர் சூரா ஃபாத்திஹா மற்றும் ஒரு சூரா ஓத வேண்டும். ருகூஃ, சுஜூத், தஷஹ்ஹுத், ஸலாம் என்று தொழுகையை முடிக்க வேண்டும். ஜமாஅத்தினர் இமாமைப் பின்பற்ற வேண்டும்; இமாம் சத்தமாக ஓதவேண்டிய தொழுகைகளில் (ஃபஜ்ர், மஃக்ரிப், இஷா முதல் இரு ரக்அத்) சத்தமாக ஓத வேண்டும்.",
                    meaningEnglish = "Leading the prayer (imam): make the intention, say 'Allahu akbar' to begin, recite Al-Fatiha and a surah, then bow, prostrate, sit for tashahhud, and end with salam. The congregation follows the imam. Recite aloud in the prayers that are read aloud (Fajr, Maghrib, and the first two rakats of Isha).",
                ),
                LearnItem(
                    id = "imam_jumma", title = "Leading Jumu'ah (Friday prayer)",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "ஜும்ஆ தொழுகை: இரண்டு குத்பாக்கள் (உரைகள்) நிகழ்த்தி, பின்னர் 2 ரக்அத் ஜும்ஆ தொழுகையை ஜமாஅத்தாக நடத்த வேண்டும். குத்பாவின் போது மவுனமாக இருக்க வேண்டும். (ஹனஃபி: முன் சுன்னத்துகளும், பின் சுன்னத்துகளும் உண்டு.)",
                    meaningEnglish = "Jumu'ah: deliver two khutbahs (sermons), then lead the 2 rakats of the Friday prayer in congregation. Stay silent during the khutbah. (Hanafi: there are sunnah rakats before and after.)",
                ),
                LearnItem(
                    id = "imam_janaza", title = "Leading the funeral prayer (janaza)",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "ஜனாசா தொழுகை: ருகூஃ, சுஜூத் இல்லை. 4 தக்பீர்கள். முதல் தக்பீருக்குப் பின் சூரா ஃபாத்திஹா ஓதவும். இரண்டாவது தக்பீருக்குப் பின் நபி (ஸல்) மீது ஸலவாத் ஓதவும். மூன்றாவது தக்பீருக்குப் பின் இறந்தவருக்காக துஆ செய்யவும். நான்காவது தக்பீருக்குப் பின் முஸ்லிம்களுக்காக துஆ செய்து, ஸலாம் கொடுக்கவும்.",
                    meaningEnglish = "Janaza: no bowing or prostration. Four takbirs. After the first, recite Al-Fatiha. After the second, send blessings on the Prophet. After the third, pray for the deceased. After the fourth, pray for all Muslims, then give salam.",
                ),
                LearnItem(
                    id = "janaza_dua", title = "Janaza dua for the deceased (male)",
                    arabic = "اللَّهُمَّ اغْفِرْ لَهُ وَارْحَمْهُ وَعَافِهِ وَاعْفُ عَنْهُ",
                    tamilTransliteration = "அல்லாஹும்மக்ஃபிர்லஹு வர்ஹம்ஹு வ ஆஃபிஹி வஃபு அன்ஹு.",
                    englishTransliteration = "Allahummaghfir lahu warhamhu wa 'aafihi wa'fu 'anhu.",
                    meaningTamil = "இறைவா! இவரை மன்னித்து, இவருக்கு கருணை காட்டு, இவரைப் பாதுகாத்து, இவரது குறைகளை மன்னித்தருள்வாயாக.",
                    meaningEnglish = "O Allah, forgive him, have mercy on him, grant him well-being, and pardon him.",
                ),
            ),
        ),
        LearnCategory(
            title = "Madhhab differences in prayer (Hanafi and Shafi'i)",
            items = listOf(
                LearnItem(
                    id = "hanafi", title = "Hanafi prayer: main points",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "ஹனஃபி முறை: தக்பீர் தஹ்ரீமாவில் மட்டும் கைகளை உயர்த்தவும். கைகளை தொப்புளுக்குக் கீழே கட்டவும். ஆமீன் மெதுவாக (சத்தமின்றி) சொல்லவும். பிஸ்மில்லாவை மெதுவாக ஓதவும். வித்ர் தொழுகையில் 3வது ரக்அத்தில் ருகூஃவுக்கு முன் துஆ கூனூத் ஓதவும்.",
                    meaningEnglish = "Hanafi: raise the hands only at the opening takbir. Place the hands below the navel. Say 'Ameen' silently. Recite 'Bismillah' silently. In Witr, recite the qunut dua in the third rakat before bowing.",
                ),
                LearnItem(
                    id = "shafii", title = "Shafi'i prayer: main points",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "ஷாஃபிஈ முறை: தக்பீர் தஹ்ரீமா, ருகூஃவுக்கு முன், ருகூஃவிலிருந்து எழும்போது கைகளை உயர்த்தவும். கைகளை மார்பின் மீது கட்டவும். சத்தமாக ஓதும் தொழுகைகளில் பிஸ்மில்லாவையும் ஆமீனையும் சத்தமாகச் சொல்லவும். ஃபஜ்ர் தொழுகையில் ருகூஃவுக்குப் பின் கூனூத் ஓதவும்.",
                    meaningEnglish = "Shafi'i: raise the hands at the opening takbir, before bowing, and when rising from bowing. Place the hands on the chest. In prayers read aloud, say 'Bismillah' and 'Ameen' aloud. In Fajr, recite the qunut after rising from bowing.",
                ),
                LearnItem(
                    id = "madhhab_note", title = "Note on these differences",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "இவை இரு மத்ஹபுகளின் முக்கிய வேறுபாடுகள். ஒவ்வொரு மத்ஹபிலும் விவரங்கள் வேறுபடும். உங்கள் குழுவின் இமாம் அல்லது ஆலிம் உறுதிப்படுத்திய முறையைப் பின்பற்றவும்.",
                    meaningEnglish = "These are the main differences between the two schools. Details vary within each school. Follow the method confirmed by your group's imam or scholar.",
                ),
            ),
        ),
        LearnCategory(
            title = "Halal and haram",
            items = listOf(
                LearnItem(
                    id = "halal", title = "Halal (permitted)",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "ஹலால்: அனுமதிக்கப்பட்டது. உதாரணம்: அறுக்கப்பட்ட ஹலாலான விலங்குகளின் இறைச்சி (அல்லாஹ்வின் பெயர் சொல்லி அறுத்தது), சைவ உணவுகள், நேர்மையான வர்த்தகம், அனுமதிக்கப்பட்ட சம்பாத்தியம்.",
                    meaningEnglish = "Halal: permitted. Examples: meat of permitted animals slaughtered with Allah's name, vegetarian food, honest trade, and lawful earnings.",
                ),
                LearnItem(
                    id = "haram", title = "Haram (forbidden)",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "ஹராம்: தடுக்கப்பட்டது. உதாரணம்: பன்றி இறைச்சி, இரத்தம், இறந்த பிராணி, மது மற்றும் போதைப் பொருட்கள், வட்டி (ரிபா), சூது (மைசிர்), மோசடி மற்றும் பிறர் பொருளை அபகரித்தல்.",
                    meaningEnglish = "Haram: forbidden. Examples: pork, blood, carrion, alcohol and intoxicants, interest (riba), gambling (maysir), fraud, and taking others' property unlawfully.",
                ),
                LearnItem(
                    id = "doubtful", title = "Doubtful (mushtabah)",
                    arabic = "", tamilTransliteration = "", englishTransliteration = "",
                    meaningTamil = "சந்தேகத்திற்குரியவை (முஷ்தபஹ்): ஹலால் அல்லது ஹராம் என்று தெளிவாகத் தெரியாதவை. தெளிவு பெறும் வரை விலகி இருப்பது நல்லது. ஸக்காத் மற்றும் ஹராம் வருமானத்தின் சுத்திகரிப்பு பற்றி ஆலிமிடம் கேட்கவும்.",
                    meaningEnglish = "Doubtful: not clearly halal or haram. It is best to avoid them until clarity is reached. Ask a scholar about zakat and purifying haram income.",
                ),
            ),
        ),
    )
}
