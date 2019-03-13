package com.mesr.bot

object Strings {
  val helloMessageStr: String =
    """
      |*کاربر گرامی* شما از طریق این بازو می توانید با حدس عکس، یک تجربه *سرگرمی هیجان انگیز* در بله داشته باشید. برای شروع این *چالش* روی *شروع* کلیک کنید.
      |[/game](send:/game) - شروع بازی
      |[/buy](send:/buy) - خرید سکه
      |[/setInvite](send:/setInvite) - وارد کردن کد دعوت
      |[/reset](send:/reset) - ریست همه چیز!
      |[/help](send:/help) - راهنما
    """.stripMargin

  val coinBuyStr: String =
    """
      |لطفا تعداد سکه‌هایی که می‌خواهید را مشخص کنید.
      |نکته: با معرفی بازو به دوستان خود می توانید به ازای هر دوست 20 سکه بگیرید
    """.stripMargin

  val enterYourResponseStr = "کلمه مورد نظر خود را وارد کن:"
  val noOfCoinsStr = "تعداد سکه شما:‌ "
  val levelStr = "مرحله "
  val guidMessageStr = "راهنما"
  val wrongGuessStr = "*حدس شما اشتباه بود*"
  val insufficientAmountStr = "سکه‌های شما کافی نیست."
  val gameFinishedStr = "شما به پایان بازی رسیدید. منتظر مراحل جدید باشید."
  val showSomeCharNotAllowedStr = "شما یک‌بار از این قابلیت استفاده کرده‌اید."
  val buyStr = "خرید سکه"
  val showSomeCharsStr = "نمایش چند حرف"
  val showWordStr = "نمایش کل کلمه"
  val showHelpStr = "راهنما"
  val returnButtonStr = "بازگشت"
  val inviteFriendsButtonStr = "دعوت دوستان"
  val enterInviteCodeStr = "لطفا کد دعوت دوست خود را وارد کنید."
  val coinBuyButtonStartStr = "خرید "
  val coinBuyButtonEndStr = " سکه"
  val inviteCodeNotNumberErrorStr = "کد دعوت باید عدد باشد. لطفا مجددا تلاش کنید."
  val alreadyInvitedErrorStr = "شما قبلا دعوت شده‌اید."
  val inviteCodeNotFoundErrorStr = "کد دعوت اشتباه است. لطفا مجددا تلاش کنید."
  val inviterSuccessStr = "شما ۲۰ امتیاز بابت دعوت دوست‌تان گرفتید."
  val inviteeSuccessStr = "شما ۲۰ امتیاز بابت استفاده از کد دعوت دوست‌تان گرفتید."

  def successfulPaymentStr(paidCoinCount: Int, newCoinCount: Int): String =
    s"""
       |شما $paidCoinCount سکه خریده‌اید.
       |تعداد کد سکه‌ها: $newCoinCount
    """.stripMargin

  def inviteStr(userId: Long): String =
    s"""
       |کد دعوت شما: $userId
       |با وارد کردن این کد توسط دوست شما، 20 سکه به حساب شما و 10 سکه به حساب دوست شما اضافه می شود.
       |
    """.stripMargin

  def showWordStr(response: String): String = "جواب: *" + response + "*"
  def wonStr(wonCoin: Int): String = "*حدس شما درست بود. شما برنده " + wonCoin + " سکه شدید.*"
  def coinBuyButtonStr(coinCount: Int): String = coinBuyButtonStartStr + coinCount + coinBuyButtonEndStr
  def coinBuyLabelStr(coinCount: Int): String = "خرید " + coinCount + " سکه بازی آفتابه"
  def coinStr(coinCount: Int): String = "تعداد سکه‌های شما: " + coinCount
}
