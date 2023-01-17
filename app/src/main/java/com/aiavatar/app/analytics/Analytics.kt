package com.pepulnow.app.analytics

@Suppress("SpellCheckingInspection")
object Analytics {

    /**
     * An `object` to hold the custom Firebase Analytics Events
     */
    object Event {
        const val ONBOARD_GET_OTP_BUTTON_EVENT = "Onboarding_Get_OTPbutton"
        const val ONBOARD_AUTOFILL_PHONE_NUMBER = "Onboarding_autoFillnumber"
        const val ONBOARD_OTP_SUCCESS_EVENT = "Onboarding_OTPsuccess"
        const val ONBOARD_OTP_FAIL_EVENT = "Onboarding_OTPfail"
        const val ONBOARD_CUSTOMIZED_FIRST_OPEN = "Customized_firstopen"
        const val ONBOARD_MOBILE_NUMBER_SCREEN_PRESENTED = "Onboard_mobile_number_screen"

        const val ONBOARD_EMAIL_VERIFY_OTP_CLICKED_EVENT = "Onboarding_email_verifyOTP"
        const val ONBOARD_EMAIL_NO_EMAIL_CLICKED_EVENT = "Onboarding_email_noemail"
        const val ONBOARD_JOIN_THE_WAITLIST = "Onboarding_jointhewaitlist"
        const val ONBOARD_JOIN_THE_WAITLIST_OPEN = "Onboarding_jointhewaitlist_signup"
        const val ONBOARD_NO_INVITE_CODE = "Onboarding_noinvitecode"
        const val ONBOARD_PROFILE_PICTURE_UPLOADED = "Onboard_profile_pic_upload"

        const val HOME_PAGE_PRESENTED = "Unique_Homepage_presented"
        const val FIRST_STORY_UPLOADED = "First_story_upload"

        /* After first profile upload success */
        const val ONBOARD_SUCCESS_EVENT = "Onboarding_success"

        /* User manually clicks the 'Home'' */
        const val HOME_FEED_EVENT = "Homefeed"

        /* User manually clicks the 'My Friends'  */
        const val HOME_FEED_MY_FRIENDS_EVENT = "Homefeed_Myfriends"

        /* User manually clicks the 'Discovery' */
        const val HOME_FEED_DISCOVERY_EVENT = "Homefeed_Discovery"

        /* User manually clicks the 'People'' */
        const val PEOPLE_MENU_EVENT = "Peoplemenu"

        /* User manually clicks the 'Friends in PeopleNow'  */
        const val PEOPLE_MENU_FRIENDS_EVENT = "Peoplemenu_Friends"

        /* User manually clicks the 'Contacts' */
        const val PEOPLE_MENU_CONTACTS_EVENT = "Peoplemenu_Contacts"

        /* User manually clicks the 'Friends of Friends' */
        const val PEOPLE_MENU_FRIENDS_OF_FRIENDS_EVENT = "Peoplemenu_FriendsofFriends"

        /* Clicks Camera Button */
        const val CAMERA_BUTTON_EVENT = "Camera_button"

        /* Moment is captured, awaiting for post description */
        const val MOMENT_CAPTURED_EVENT = "Moment_capture"

        /* Clicks 'Next' after Post Description */
        const val MOMENT_CAPTURED_STEP_2_EVENT = "Moment_capture_step2"

        /* Successfully uploads the Moment */
        const val MOMENT_CAPTURE_FINAL_UPLOAD_EVENT = "Moment_capture_finalupload"

        const val MOMENT_CAPTURE_MY_FRIENDS_TOGGLE = "Moment_capture_myfriends"
        const val MOMENT_CAPTURE_EVERYONE_TOGGLE = "Moment_capture_Everyone"
        const val MOMENT_CAPTURE_HIDE_MY_MOMENT_TOGGLE = "Moment_capture_Hidemymoment"
        const val MOMENT_CAPTURE_LOCATION_TOGGLE = "Moment_capture_sharelocation"
        const val MOMENT_CAPTURE_HIGHLIGHTS_TOGGLE = "Moment_capture_addtohighlights"

        /* Clicks 'Notifications'  */
        const val NOTIFICATIONS_MENU_EVENT = "Notifications_menu"

        /* Clicks 'Profile' */
        const val MY_PROFILE_EVENT = "Myprofile"

        /* Manually clicks the 'Today's Moment' */
        const val MY_PROFILE_TODAYS_MOMENT_EVENT = "Myprofile_Todays_Moments"

        /* Manually clicks the 'Memories' */
        const val MY_PROFILE_MEMORIES_EVENT = "Myprofile_Memories"

        const val SHARE_MY_PROFILE_EVENT = "Myprofile_Sharemyprofile"

        const val MY_PROFILE_MENU_BAR_EVENT = "Myprofile_menubar"

        const val MY_PROFILE_MENU_BAR_EDIT_PROFILE_EVENT = "Myprofile_menubar_Editprofile"
        const val MY_PROFILE_MENU_BAR_SHARE_EVENT = "Myprofile_menubar_shareprofile"
        const val MY_PROFILE_MENU_BAR_STATUS_PRIVACY_EVENT = "Myprofile_menubar_Statusprivacy"
        const val MY_PROFILE_MENU_BAR_SETTINGS_EVENT = "Myprofile_menubar_settings"
        const val MY_PROFILE_MY_REWARDS_CLICK_EVENT = "Myprofile_Myrewards"
        const val MY_PROFILE_MY_REWARDS_MOMENT_STREAK_SCREEN = "Myprofile_Myrewards_Momentstreak"
        const val MY_PROFILE_MY_REWARDS_REFERRAL_PROGRAM_SCREEN =
            "Myprofile_Myrewards_Referralprogram"

        const val MY_PROFILE_FRIENDS_CLICKED_EVENT = "Myprofile_Friends"
        const val MY_PROFILE_FOLLOWERS_CLICKED_EVENT = "Myprofile_Followers"
        const val MY_PROFILE_FOLLOWING_CLICKED_EVENT = "Myprofile_following"

        const val BLUR_VIEW_POST_MY_MOMENTS_EVENT = "Blurredview_postmymoments"

        const val MOMENT_REACTION_BUTTON_CLICK_EVENT = "Moment_reaction"
        const val MOMENT_REACTION_PHOTO_CAPTURE_EVENT = "Moment_reaction_photocapture"
        const val MOMENT_REACTION_PHOTO_CAPTURE_FAILED_EVENT = "Moment_reaction_photocapture_failed"
        const val MOMENT_REACTION_POSTED_EVENT = "Moment_reaction_posted"
        const val MOMENT_REACTION_SMILEY_CLICK_EVENT = "Moment_reaction_smilies"

        const val OTHER_PROFILE_FROM_MY_FRIENDS_FEED_EVENT = "Myfriends_othersprofile"
        const val OTHER_PROFILE_FROM_DISCOVERY_FEED_EVENT = "Discovery_othersprofile"
        const val OTHER_PROFILE_FROM_FRIENDS_IN_PEOPLE_NOW_EVENT = "Friendsinpepulnow_othersprofile"
        const val OTHER_PROFILE_FROM_FRIENDS_OF_FRIENDS_EVENT = "Friendsoffriends_othersprofile"

        const val OTHER_PROFILE_ADD_FRIEND_ACTION_EVENT = "othersprofile_addfriend"
        const val OTHER_PROFILE_FOLLOW_ACTION_EVENT = "othersprofile_addfollow"
        const val OTHER_PROFILE_UNFRIEND_ACTION_EVENT = "othersprofile_unfriend"
        const val OTHER_PROFILE_UNFOLLOW_ACTION_EVENT = "othersprofile_unfollow"

        /* Manually clicks 'Today's Moments' on Others Profile */
        const val OTHER_PROFILE_TODAY_MOMENT_EVENT = "othersprofile_todaymoments"

        /* Manually clicks 'Highlights' on Others Profile */
        const val OTHER_PROFILE_HIGHLIGHTS_EVENT = "othersprofile_Highlights"

        const val OTHER_PROFILE_OPTION_BUTTON_EVENT = "othersprofile_elipsis"
        const val OTHER_PROFILE_MUTE_ACTION_EVENT = "othersprofile_Mute"
        const val OTHER_PROFILE_UN_MUTE_ACTION_EVENT = "othersprofile_Unmute"
        const val OTHER_PROFILE_REPORT_ACTION_EVENT = "othersprofile_Report"
        const val OTHER_PROFILE_BLOCK_ACTION_EVENT = "othersprofile_block"
        const val OTHER_PROFILE_ASK_MOMENT_ACTION_EVENT = "othersprofile_askmoment"
        const val OTHER_PROFILE_MUTUAL_FRIENDS_CLICK_EVENT = "othersprofile_mutualfriends"

        /* When user clicks 'Post My Moments' from Moment Streak Page. */
        const val POST_MOMENT_FROM_MOMENT_STREAK_PAGE = "Postmoment_insidemomentstreak"

        /* User clicks promotion and successfully posts the Moment */
        const val PUSH_NOTIFICATION_MOMENT_POSTED_EVENT = "timingnotifications_momentposted"

        /* User clicked 'Add Moment' from Moments Request in notification page */
        const val NOTIFICATION_ADD_MOMENT_EVENT = "asknotifications_momentposted"

        /* Other Push Notifications click */
        const val PUSH_NOTIFICATION_CLICK_EVENT = "otherpushnotifications"

        const val INVITE_FROM_PROFILE_VISITORS = "Blurredview_invite1friend"

        const val REFERRAL_PAGE_COPY_LINK = "Referral_copy"
        const val REFERRAL_PAGE_SHARE_TO_WHATSAPP = "Referral_Whatsapp"

        /* User clicks on the moment streak button on home feeds */
        const val HOME_SCREEN_GIFT_ICON_CLICK = "Homescreen_gifticon"
        const val HOME_SCREEN_SEARCH_CLICK = "Homescreen_search"

        const val MOMENT_STREAK_INVITE_NOW = "Momentstreak_invitenow"
        const val MOMENT_STREAK_STREAK_WINNERS = "Momentstreak_streakwinners"

        const val REFERRAL_CONTEST_PREVIOUS_WINNERS = "Referralcontest_previouswinners"
        const val REFERRAL_CONTEST_SCORE = "Referralcontest_score"

        const val DELETE_ACCOUNT_GET_OTP_CLICKED = "DeleteMyAccount_getOtpClicked"

        /* Chat */
        const val HOME_PAGE_CHAT_ICON_CLICK = "Homepage_chatIcon_click"

        const val FEEDS_DIRECT_REPLY_CLICK  = "Feeds_directReply_click"
        const val FEEDS_DIRECT_REPLY_SEND   = "Feeds_directReply_send"
        const val FEEDS_DIRECT_REPLY_DOUBLE_TAP_GESTURE = "Feeds_directReply_doubleTap"

        const val CHATS_THREAD_FEEDBACK_CLICK = "ChatList_feedback_click"

        const val CHATS_NEW_CHAT_CLICK = "ChatList_newChat_click"
        const val NEW_CHAT_FRIENDS_TAB_SWITCH = "NewChat_friends_tab"
        const val NEW_CHAT_LOCAL_TAB_SWITCH = "NewChat_local_tab"
        const val NEW_CHAT_CREATE_CHAT_CLICK = "NewChat_createChat_click"

        const val MESSAGES_THREAD_PRESENTED = "Messages_thread_presented"
        const val MESSAGES_SEND_MESSAGE     = "Messages_send_message"
        const val MESSAGES_THREAD_OPTION_BUTTON_CLICK = "Messages_optBtn_click"
        const val MESSAGES_THREAD_OPTION_REPORT_CLICK = "Messages_option_reportUser_click"
        const val MESSAGES_THREAD_OPTION_BLOCK_UNBLOCK_CLICK = "Messages_option_blockUnblock_click"
        const val MESSAGES_THREAD_OPTION_MUTE_UNMUTE_CLICK = "Messages_option_mute_unmute_click"
        const val PROFILE_PAGE_FROM_MESSAGE_THREAD = "Profile_from_messageThread"

        /* END - Chat */
    }

    object OtherEvents {
        const val DEEP_LINK_INVITATION = "deepLink_invitation"
    }

    object ErrEvents {
        const val UNCAUGHT_API_FAILURE = "uncaught_api_failure"
    }
}

@Suppress("SpellCheckingInspection")
val ACTIVE_ADJUST_EVENT_TOKEN_MAP = mapOf(
    Analytics.Event.ONBOARD_GET_OTP_BUTTON_EVENT to "8ehms7",
    Analytics.Event.ONBOARD_OTP_SUCCESS_EVENT to "p8vepo",
    Analytics.Event.ONBOARD_OTP_FAIL_EVENT to "jdjv78",
    Analytics.Event.ONBOARD_CUSTOMIZED_FIRST_OPEN to "4yiayx",

    Analytics.Event.ONBOARD_MOBILE_NUMBER_SCREEN_PRESENTED to "1ngipn",
    Analytics.Event.ONBOARD_PROFILE_PICTURE_UPLOADED to "3lzdv3",
    Analytics.Event.HOME_PAGE_PRESENTED to "gj6m9k" ,
    Analytics.Event.FIRST_STORY_UPLOADED to "n5ci52",

    /* After first profile upload success */
    Analytics.Event.ONBOARD_SUCCESS_EVENT to "x6czwa",
)

@Suppress("SpellCheckingInspection")
val ADJUST_EVENT_TOKEN_MAP = mapOf(
    Analytics.Event.ONBOARD_GET_OTP_BUTTON_EVENT to "8ehms7",
    Analytics.Event.ONBOARD_AUTOFILL_PHONE_NUMBER to "2ncn2w",
    Analytics.Event.ONBOARD_OTP_SUCCESS_EVENT to "p8vepo",
    Analytics.Event.ONBOARD_OTP_FAIL_EVENT to "jdjv78",
    Analytics.Event.ONBOARD_CUSTOMIZED_FIRST_OPEN to "4yiayx",

    Analytics.Event.ONBOARD_EMAIL_VERIFY_OTP_CLICKED_EVENT to "eo1srs",
    Analytics.Event.ONBOARD_EMAIL_NO_EMAIL_CLICKED_EVENT to "m5ink6",
    Analytics.Event.ONBOARD_JOIN_THE_WAITLIST to "lujiss",
    Analytics.Event.ONBOARD_JOIN_THE_WAITLIST_OPEN to "j617p8",
    Analytics.Event.ONBOARD_NO_INVITE_CODE to "t0crqc",

    Analytics.Event.ONBOARD_MOBILE_NUMBER_SCREEN_PRESENTED to "1ngipn",
    Analytics.Event.ONBOARD_PROFILE_PICTURE_UPLOADED to "3lzdv3",
    Analytics.Event.HOME_PAGE_PRESENTED to "gj6m9k" ,
    Analytics.Event.FIRST_STORY_UPLOADED to "n5ci52",

    /* After first profile upload success */
    Analytics.Event.ONBOARD_SUCCESS_EVENT to "x6czwa",

    /* User manually clicks the 'Home'' */
    Analytics.Event.HOME_FEED_EVENT to "uumj5z",

    /* User manually clicks the 'My Friends'  */
    Analytics.Event.HOME_FEED_MY_FRIENDS_EVENT to "974lxj",

    /* User manually clicks the 'Discovery' */
    Analytics.Event.HOME_FEED_DISCOVERY_EVENT to "4rfjn8",

    /* User manually clicks the 'People'' */
    Analytics.Event.PEOPLE_MENU_EVENT to "fiwjjk",

    /* User manually clicks the 'Friends in PeopleNow'  */
    Analytics.Event.PEOPLE_MENU_FRIENDS_EVENT to "wr43i1",

    /* User manually clicks the 'Contacts' */
    Analytics.Event.PEOPLE_MENU_CONTACTS_EVENT to "5kuijx",

    /* User manually clicks the 'Friends of Friends' */
    Analytics.Event.PEOPLE_MENU_FRIENDS_OF_FRIENDS_EVENT to "vcs6lk",

    /* Clicks Camera Button */
    Analytics.Event.CAMERA_BUTTON_EVENT to "dga3rw",

    /* Moment is captured, awaiting for post description */
    Analytics.Event.MOMENT_CAPTURED_EVENT to "blgyy3",

    /* Clicks 'Next' after Post Description */
    Analytics.Event.MOMENT_CAPTURED_STEP_2_EVENT to "fihnmr",

    /* Successfully uploads the Moment */
    Analytics.Event.MOMENT_CAPTURE_FINAL_UPLOAD_EVENT to "vhx45l",

    Analytics.Event.MOMENT_CAPTURE_MY_FRIENDS_TOGGLE to "b95hpi",
    Analytics.Event.MOMENT_CAPTURE_EVERYONE_TOGGLE to "7qt20c",
    Analytics.Event.MOMENT_CAPTURE_HIDE_MY_MOMENT_TOGGLE to "xopik3",
    Analytics.Event.MOMENT_CAPTURE_LOCATION_TOGGLE to "qoq5kj",
    Analytics.Event.MOMENT_CAPTURE_HIGHLIGHTS_TOGGLE to "f6rf0i",

    /* Clicks 'Notifications'  */
    Analytics.Event.NOTIFICATIONS_MENU_EVENT to "8yxoab",

    /* Clicks 'Profile' */
    Analytics.Event.MY_PROFILE_EVENT to "lu1vyv",

    /* Manually clicks the 'Today's Moment' */
    Analytics.Event.MY_PROFILE_TODAYS_MOMENT_EVENT to "mky21y",

    /* Manually clicks the 'Memories' */
    Analytics.Event.MY_PROFILE_MEMORIES_EVENT to "k53jfs",

    Analytics.Event.SHARE_MY_PROFILE_EVENT to "ljc0no",

    Analytics.Event.MY_PROFILE_MENU_BAR_EVENT to "nz7hld",

    Analytics.Event.MY_PROFILE_MENU_BAR_EDIT_PROFILE_EVENT to "6u0pqm",
    Analytics.Event.MY_PROFILE_MENU_BAR_SHARE_EVENT to "papb45",
    Analytics.Event.MY_PROFILE_MENU_BAR_STATUS_PRIVACY_EVENT to "n9mibt",
    Analytics.Event.MY_PROFILE_MENU_BAR_SETTINGS_EVENT to "rgx9p8",
    Analytics.Event.MY_PROFILE_MY_REWARDS_CLICK_EVENT to "2gf9f7",
    Analytics.Event.MY_PROFILE_MY_REWARDS_MOMENT_STREAK_SCREEN to "tcfexe",
    Analytics.Event.MY_PROFILE_MY_REWARDS_REFERRAL_PROGRAM_SCREEN to "2ovkw9",

    Analytics.Event.MY_PROFILE_FRIENDS_CLICKED_EVENT to "b1ehdz",
    Analytics.Event.MY_PROFILE_FOLLOWERS_CLICKED_EVENT to "2bawub",
    Analytics.Event.MY_PROFILE_FOLLOWING_CLICKED_EVENT to "4kdcnj",

    Analytics.Event.BLUR_VIEW_POST_MY_MOMENTS_EVENT to "w5qi89",

    Analytics.Event.MOMENT_REACTION_BUTTON_CLICK_EVENT to "xkzi7g",
    Analytics.Event.MOMENT_REACTION_PHOTO_CAPTURE_EVENT to "64ebb4",
    Analytics.Event.MOMENT_REACTION_PHOTO_CAPTURE_FAILED_EVENT to "qrumja",
    Analytics.Event.MOMENT_REACTION_POSTED_EVENT to "7rwl85",
    Analytics.Event.MOMENT_REACTION_SMILEY_CLICK_EVENT to "7h41qc",

    Analytics.Event.OTHER_PROFILE_FROM_MY_FRIENDS_FEED_EVENT to "qfbuks",
    Analytics.Event.OTHER_PROFILE_FROM_DISCOVERY_FEED_EVENT to "hd14zl",
    Analytics.Event.OTHER_PROFILE_FROM_FRIENDS_IN_PEOPLE_NOW_EVENT to "9jjt5k",
    Analytics.Event.OTHER_PROFILE_FROM_FRIENDS_OF_FRIENDS_EVENT to "sfs6yu",

    Analytics.Event.OTHER_PROFILE_ADD_FRIEND_ACTION_EVENT to "9su0xc",
    Analytics.Event.OTHER_PROFILE_FOLLOW_ACTION_EVENT to "4flbb0",
    Analytics.Event.OTHER_PROFILE_UNFRIEND_ACTION_EVENT to "oghazd",
    Analytics.Event.OTHER_PROFILE_UNFOLLOW_ACTION_EVENT to "7obhv4",

    /* Manually clicks 'Today's Moments' on Others Profile */
    Analytics.Event.OTHER_PROFILE_TODAY_MOMENT_EVENT to "1txd5t",

    /* Manually clicks 'Highlights' on Others Profile */
    Analytics.Event.OTHER_PROFILE_HIGHLIGHTS_EVENT to "o5d4ik",

    Analytics.Event.OTHER_PROFILE_OPTION_BUTTON_EVENT to "atqm1l",
    Analytics.Event.OTHER_PROFILE_MUTE_ACTION_EVENT to "yxu1iu",
    Analytics.Event.OTHER_PROFILE_UN_MUTE_ACTION_EVENT to "4rnf3n",
    Analytics.Event.OTHER_PROFILE_REPORT_ACTION_EVENT to "5g12kt",
    Analytics.Event.OTHER_PROFILE_BLOCK_ACTION_EVENT to "kr3b48",
    Analytics.Event.OTHER_PROFILE_ASK_MOMENT_ACTION_EVENT to "ffjwds",
    Analytics.Event.OTHER_PROFILE_MUTUAL_FRIENDS_CLICK_EVENT to "dh5086",

    /* When user clicks 'Post My Moments' from Moment Streak Page. */
    Analytics.Event.POST_MOMENT_FROM_MOMENT_STREAK_PAGE to "l2xfk3",

    /* User clicks promotion and successfully posts the Moment */
    Analytics.Event.PUSH_NOTIFICATION_MOMENT_POSTED_EVENT to "xxfffm",

    /* User clicked 'Add Moment' from Moments Request in notification page */
    Analytics.Event.NOTIFICATION_ADD_MOMENT_EVENT to "wcory8",

    /* Other Push Notifications click */
    Analytics.Event.PUSH_NOTIFICATION_CLICK_EVENT to "es5rrq",

    Analytics.Event.INVITE_FROM_PROFILE_VISITORS to "dnp7wn",

    Analytics.Event.REFERRAL_PAGE_COPY_LINK to "vd4d73",
    Analytics.Event.REFERRAL_PAGE_SHARE_TO_WHATSAPP to "o9k5zx",

    Analytics.Event.HOME_SCREEN_GIFT_ICON_CLICK to "3ownf1",
    Analytics.Event.HOME_SCREEN_SEARCH_CLICK to "vlb4dj",

    Analytics.Event.MOMENT_STREAK_INVITE_NOW to "8lbl0e",
    Analytics.Event.MOMENT_STREAK_STREAK_WINNERS to "z294em",

    Analytics.Event.REFERRAL_CONTEST_PREVIOUS_WINNERS to "9owvwe",
    Analytics.Event.REFERRAL_CONTEST_SCORE to "gvmu7p",

    Analytics.Event.DELETE_ACCOUNT_GET_OTP_CLICKED to "hd30kt",
)