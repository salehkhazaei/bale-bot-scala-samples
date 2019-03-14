package com.mesr.bot

object Strings {
  val helloMessageStr: String = "*کاربر گرامی* شما از طریق این بازو می توانید با حدس عکس، یک تجربه *سرگرمی هیجان انگیز* در بله داشته باشید. برای شروع این *چالش* روی *شروع* کلیک کنید."

  val coinBuyStr: String =
    """
      |لطفا تعداد آفتابه‌هایی که می‌خواهید را مشخص کنید.
      |نکته: با معرفی بازو به دوستان خود می توانید به ازای هر دوست یک دست آفتابه لگن بگیرید
    """.stripMargin.trim

  val enterYourResponseStr = "کلمه مورد نظر خود را *وارد کن* :"
  val noOfCoinsStr = "تعداد آفتابه‌های شما:‌ "
  val levelStr = "مرحله "
  val guidMessageStr = "کاربر گرامی شما با استفاده از این بازو می توانید یک بازی تعاملیِ سرگرم کننده را تجربه کنید.\nدر هر مرحله شما بایستی کلمه مرتبط با تصاویر نشان دادن شده را حدس بزنید و در صورتی که تمایل داشته باشید با خرج کردن چند آفتابه راهنمایی دریافت کنید.\nبرای نمایش چند حرف *یک آفتابه قرمز* و برای نمایش کل کلمه شما بایستی *یک آفتابه حلبی* بپردازید.\nبه ازای دعوت هر دوستی که کد دعوت شما را وارد کند *یک دست آفتابه لگن* هدیه می گیرید."
  val wrongGuessStr = "*حدس شما اشتباه بود*"
  val insufficientAmountStr = "آفتابه‌های شما کافی نیست."
  val gameFinishedStr = "شما به پایان بازی رسیدید. منتظر مراحل جدید باشید."
  val showSomeCharNotAllowedStr = "شما یک‌بار از این قابلیت استفاده کرده‌اید."
  val buyStr = "خرید آفتابه"
  val showSomeCharsStr = "نمایش چند حرف"
  val showWordStr = "نمایش کل کلمه"
  val showHelpStr = "راهنما"
  val showInviteCodeStr = "وارد کردن کد دعوت"
  val returnButtonStr = "بازگشت"
  val startButtonStr = "شروع بازی"
  val addingNewLevelStr = "پیشنهاد مرحله جدید"
  val inviteFriendsButtonStr = "دعوت دوستان"
  val enterInviteCodeStr = "لطفا کد دعوت دوست خود را وارد کنید."
  val aftabeGhermezBuyStr = "خرید " + getCoinString(1)
  val aftabeHalabiBuyStr = "خرید " + getCoinString(2)
  val aftabeLaganBuyStr = "خرید " + getCoinString(4)
  val aftabeLagan7DastBuyStr = "خرید " + getCoinString(8)
  val aftabeGhermezDescStr = "آفتابه قرمز: ۵۰ ریال"
  val aftabeHalabiDescStr = "آفتابه حلبی معادل ۲ آفتابه قرمز: ۹۰ ریال"
  val aftabeLaganDescStr = "یک دست آفتابه لگن معادل ۴ آفتابه قرمز: ۱۵۰ ریال"
  val aftabeLagan7DastDescStr = "یک بسته ۷ دست آفتابه لگن معادل ۸ آفتابه قرمز: ۲۰۰ ریال"
  val inviteCodeNotNumberErrorStr = "کد دعوت باید عدد باشد. لطفا مجددا تلاش کنید."
  val alreadyInvitedErrorStr = "شما قبلا دعوت شده‌اید."
  val inviteCodeNotFoundErrorStr = "کد دعوت اشتباه است. لطفا مجددا تلاش کنید."
  val inviterSuccessStr = "شما *یک دست آفتابه لگن* بابت دعوت دوست‌تان گرفتید."
  val inviteeSuccessStr = "شما *یک دست آفتابه لگن* بابت استفاده از کد دعوت دوست‌تان گرفتید."
  val sendLevelPhotoStr = "لطفا عکس مرحله را ارسال کنید"
  val sendLevelResponseStr = "لطفا پاسخ مرحله را ارسال کنید"
  val levelRegisteredStr = "مرحله ثبت شد. پس از تایید ادمین به بات اضافه می‌شود."

  def successfulPaymentStr(paidCoinCount: Int, newCoinCount: Int): String =
    s"""
       |شما * ${getCoinString(paidCoinCount)} * خریدید.
       |مجموع آفتابه‌های شما: *${getCoinString(newCoinCount)} *
    """.stripMargin.trim

  def inviteStr(userId: Long): String =
    s"""
       |کد دعوت شما: $userId
       |با وارد کردن این کد توسط دوست شما، یک دست آفتابه لگن به حساب شما و یک دست آفتابه لگن به حساب دوست شما اضافه می شود.
       |
    """.stripMargin.trim

  def showWordStr(response: String): String = "جواب: *" + response + "*"
  def wonStr(wonCoin: Int): String = "*حدس شما درست بود. شما برنده " + wonCoin + " آفتابه شدید.*"

  def getCoinEnumSeq(count: Int): List[Int] = {
    val type1 = count / 8 // 7-dast

    val type2 = (count - (type1 * 8)) / 4 // 1-dast

    val type3 = (count - (type1 * 8 + type2 * 4)) / 2 // halabi

    val type4 = count - (type1 * 8 + type2 * 4 + type3 * 2) // ghermez

    List(type1, type2, type3, type4)
  }

  def getCoinString(count: Int): String = {
    val str = getCoinEnumSeq(count) match {
      case type1 :: type2 :: type3 :: type4 :: Nil =>
        val str1 = if(type1 > 0) Some(type1 + " بسته هفت دست آفتابه لگن") else None
        val str2 = if(type2 > 0) Some("یک دست آفتابه لگن") else None
        val str3 = if(type3 > 0) Some("ی دونه آفتابه حلبی") else None
        val str4 = if(type4 > 0) Some("ی دونه آفتابه قرمز") else None

        Seq(str1, str2, str3, str4).flatten.mkString(" و ")
      case _ => ""
    }

    if (str.trim.nonEmpty) "هیچی آفتابه نداری!" else str.trim
  }

  def coinBuyButtonStr(coinCount: Int): String = "خرید " + getCoinString(coinCount)
  def coinBuyLabelStr(coinCount: Int): String = "خرید " + getCoinString(coinCount)
  def coinStr(coinCount: Int): String = "تعداد آفتابه‌های شما: " + getCoinString(coinCount)
  def getCoinDescription(coinCount: Int): String = coinCount match {
    case 1 => aftabeGhermezDescStr
    case 2 => aftabeHalabiDescStr
    case 4 => aftabeLaganDescStr
    case 8 => aftabeLagan7DastDescStr
  }
}
