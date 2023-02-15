package com.aiavatar.app.analytics

import com.aiavatar.app.core.data.source.local.model.UploadSessionWithFilesEntity

@Suppress("SpellCheckingInspection")
object Analytics {

    /**
     * An `object` to hold the custom Firebase Analytics Events
     */
    object Event {
        /* Onboard */
        const val ONBOARD_GET_OTP_BUTTON_EVENT = "Onboarding_Get_OTPbutton"
        const val ONBOARD_AUTOFILL_PHONE_NUMBER = "Onboarding_autoFillnumber"
        const val ONBOARD_OTP_SUCCESS_EVENT = "Onboarding_OTPsuccess"
        const val ONBOARD_OTP_FAIL_EVENT = "Onboarding_OTPfail"
        const val ONBOARD_CUSTOMIZED_FIRST_OPEN = "Customized_firstopen"
        const val ONBOARD_MOBILE_NUMBER_SCREEN_PRESENTED = "Onboard_mobile_number_screen"

        const val ONBOARD_WELCOME_SWIPE_ACTION = "Onboard_welcome_swipeAction"
        const val ONBOARD_WELCOME_BTN_CLICK = "Onboard_welcomeBtn_click"
        const val ONBOARD_LEGAL_ACCEPT_CLICK = "Onboard_legal_acceptClick"
        const val ONBOARD_LEGAL_READ_TERMS = "Onboard_legal_read_terms"
        const val ONBOARD_LEGAL_READ_PRIVACY = "Onboard_legal_read_privacy"
        /* END - Onboard */

        /* Upload Steps */
        const val UPLOAD_STEP_SKIP_CLICK = "Upload_step_skip_click"
        const val UPLOAD_STEP_1_CONTINUE_BTN_CLICK = "Upload_step1_continueBtn_click"
        const val UPLOAD_STEP_2_UPLOAD_PHOTOS_CLICK = "Upload_step2_uploadBtn_click"
        const val UPLOAD_STEP_2_NAVIGATION_BACK_CLCK = "Upload_step2_navigationBack_click"
        const val UPLOAD_STEP_2_MORE_BTN_CLICK = "Upload_step2_moreBtn_click"
        const val UPLOAD_STEP_2_DELETE_SINGLE_PHOTO_BTN_CLICK = "Upload_step2_deleteSingleBtn_click"
        const val UPLOAD_STEP_2_NEXT_CLICK = "Upload_step2_next_click"
        const val UPLOAD_STEP_3_GENDER_TOGGLE = "Upload_step3_gender_toggle"
        const val UPLOAD_STEP_3_NEXT_CLICK = "Upload_step3_next_click"
        const val UPLOAD_STEP_3_NAVIGATION_BACK_CLICK = "Upload_step3_navigationBack_click"
        /* END - Upload Steps */

        /* Avatar Status */
        const val AVATAR_STATUS_PAGE_PRESENTED = "Avatarstatus_page_presented"
        const val AVATAR_STATUS_NOTIFY_ME_TOGGLE = "Avatarstatus_notifyMe_toggle"
        const val AVATAR_STATUS_VIEW_RESULTS_CLICK = "Avatarstatus_viewResults_click"
        const val AVATAR_STATUS_CONTINUE_CLICK = "Avatarstatus_continue_click"
        const val AVATAR_STATUS_CLOSE_BTN_CLICK = "Avatarstatus_closeBtn_click"
        /* END - Avatar Status */

        /* Avatar Results */
        const val AVATAR_RESULTS_PAGE_PRESENTED = "Avatarresult_page_presented"
        const val AVATAR_RESULTS_SHARE_BTN_CLICK = "Avatarresult_shareBtn_click"
        const val AVATAR_RESULTS_DOWNLOAD_BTN_CLICK = "Avatarresult_downloadBtn_click"
        const val AVATAR_RESULTS_CLOSE_BTN_CLICK = "Avatarresult_closeBtn_click"
        const val AVATAR_RESULTS_MODEL_ITEM_CLICK = "Avatarresult_modelItem_click"
        /* END - Avatar Results */

        /* Avtar Preview */
        const val AVATAR_PREVIEW_PAGE_PRESENTED = "Avatarpreview_page_presented"
        const val AVATAR_PREVIEW_SCROLLER_ITEM_CLICK = "Avatarpreview_scrollerItem_click"
        const val AVATAR_PREVIEW_BACK_BTN_CLICK = "Avatarpreview_backBtn_click"
        const val AVATAR_PREVIEW_SHARE_BTN_CLICK = "Avatarpreview_shareBtn_click"
        const val AVATAR_PREVIEW_DOWNLOAD_BTN_CLICK = "Avatarpreview_downloadBtn_click"
        /* END - Avtar Preview */

        /* Login */
        const val LOGIN_PAGE_PRESENTED = "Login_page_presented"
        const val LOGIN_GET_OTP_CLICK = "Login_getOtp_click"
        const val LOGIN_SOCIAL_GOOGLE_CLICK = "Login_social_google_click"
        const val LOGIN_VERIFY_OTP_CLICK = "Login_verifyOtp_click"
        const val LOGIN_BACK_ACTION_CLICK = "Login_backAction_click"
        const val LOGIN_RESEND_OTP = "Login_resend_otp_event"
        const val LOGIN_SUCCESS_EVENT = "Login_success_event"
        const val LOGIN_SOCIAL_SUCCESS_EVENT = "Login_socialLogin_success_event"
        const val LOGIN_PREVIOUS_ACCOUNT_GOOGLE_PRESENTED = "Login_previousAccount_google_presented"
        const val LOGIN_PREVIOUS_ACCOUNT_GOOGLE_CANCELED = "Login_previousAccount_google_canceled"
        const val LOGIN_PREVIOUS_ACCOUNT_GOOGLE_SIGN_IN = "Login_previousAccount_google_signIn"
        /* END - Login */

        /* Subscriptions Page */
        const val SUBSCRIPTION_PAGE_PRESENTED = "Subscriptionpage_presented"
        const val SUBSCRIPTION_PAGE_CLOSE_BTN_CLICK = "Subscriptionpage_closeBtn_click"
        const val SUBSCRIPTION_PAGE_PLAN_SELECTION_TOGGLE = "Subscriptionpage_planSelection_toggle"
        const val SUBSCRIPTION_PAGE_CONTINUE_CLICK = "Subscriptionpage_continueBtn_click"

        // TODO: add pending payment events

        const val SUBSCRIPTION_SUCCESS_PRESENTED = "Subscription_success_presented"
        /* END - Subscriptions Page */

        /* Settings */
        const val SETTIGNS_BACK_ACTION_CLICK = "Settings_backAction_click"
        const val SETTINGS_LOGOUT_CLICK = "Settings_logout_click"
        /* END - Settings */

        /* Profile */
        const val PROFILE_PAGE_PRESENTED = "Profile_page_presented"
        const val PROFILE_BACK_ACTION_CLICK = "Profile_backAction_click"
        const val PROFILE_MENU_SETTINGS_BTN_CLICK = "Profile_menu_settings_click"
        const val PROFILE_MODEL_ITEM_CLICK = "Profile_modelItem_click"
        const val PROFILE_CREATE_CLICK = "Profile_create_click"
        /* END - Profile */

        /* Landing page */
        const val LANDING_PAGE_PRESENTED = "Unique_landingPage_presented"
        const val LANDING_MENU_ACCOUNT_CLICK = "Landing_menu_account_click"
        const val LANDING_EXPLORE_MORE_CLICK = "Landing_exploreMore_click"
        const val LANDING_ALREADY_HAVE_AN_ACCOUNT_CLICK = "Landing_alreadyHaveAnAccount_click"
        const val LANDING_CREATE_BTN_CLICK = "Landing_createBtn_click"
        /* END - Landing page */

        /* Home page */
        const val HOME_PAGE_PRESENTED = "Unique_Homepage_presented"
        const val CATALOG_PRESENTED = "Catalog_page_presented"
        const val CATALOG_ITEM_CLICK = "Catalog_item_click"
        const val CATALOG_CREATE_CLICK = "Catalog_create_click"
        const val CATALOG_PROFILE_ICON_CLICK = "Catalog_profileIcon_click"
        /* END - Home page */

        /* More catalog */
        const val MORE_CATALOG_CREATE_CLICK = "Morecatalog_create_click"
        const val MORE_CATALOG_BACK_ACTION_CLICK = "Morecatalog_backAction_click"
        /* END - More catalog */

        /* Model list */
        const val MODEL_LIST_PRESENTED = "Modellist_page_presented"
        const val MODEL_LIST_BACK_ACTION_CLICK = "Modellist_backAction_click"
        const val MODEL_LIST_RECREATE_CLICK = "Modllist_recreateBtn_click"
        const val MODEL_LIST_DOWNLOAD_CLICK = "Modellist_downloadBtn_click"
        const val MODEL_LIST_SHARE_CLICK = "Modellist_shareBtn_click"
        const val MODEL_LIST_ITEM_CLICK = "Modellist_item_click"
        const val MODEL_LIST_FOLDER_NAME_CHANGE = "Modellist_folderName_change_event"
        /* END - Model list */

        /* Model detail */
        const val MODEL_DETAIL_PRESENTED = "Modeldetail_page_presented"
        const val MODEL_DETAIL_SCROLL_ACTION = "Modeldetail_scroll_action"
        const val MODEL_DETAIL_SCROLLER_ITEM_CLICK = "Modeldetail_scrollerItem_click"
        const val MODEL_DETAIL_BACK_BTN_CLICK = "Modeldetail_backBtn_click"
        const val MODEL_DETAIL_SHARE_BTN_CLICK = "Modeldetail_shareBtn_click"
        const val MODEL_DETAIL_DOWNLOAD_BTN_CLICK = "Modeldetail_downloadBtn_click"
        const val MODEL_DETAIL_FOLDER_NAME_CHANGE = "Modeldetail_folderName_change_event"
        /* END - Model detail */

        /* After first profile upload success */
        const val ONBOARD_SUCCESS_EVENT = "Onboarding_success"

        /* User manually clicks the 'Home'' */
        const val HOME_FEED_EVENT = "Homefeed"

        /* Clicks 'Notifications'  */
        const val NOTIFICATIONS_MENU_EVENT = "Notifications_menu"
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

    /* After first profile upload success */
    Analytics.Event.ONBOARD_SUCCESS_EVENT to "x6czwa",

    /* User manually clicks the 'Home'' */
    Analytics.Event.HOME_FEED_EVENT to "uumj5z",
)