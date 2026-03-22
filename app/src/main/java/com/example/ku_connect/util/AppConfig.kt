package com.example.ku_connect.util

object AppConfig {
    const val CLOUDINARY_CLOUD_NAME = "CLOUDINARY"
    const val CLOUDINARY_API_KEY    = "CLOUDINARY_API_KEY"
    const val CLOUDINARY_API_SECRET = "CLOUDINARY_API_SECRET"
    const val CLOUDINARY_UPLOAD_PRESET = "posts_upload"

    const val ALLOWED_EMAIL_DOMAIN = "@ku.th"
    const val MIN_PASSWORD_LENGTH  = 8

    const val COLLECTION_USERS   = "users"
    const val COLLECTION_POSTS   = "posts"
    const val COLLECTION_COMMENTS = "comments"
    const val COLLECTION_MARKET  = "market_items"

    const val DEFAULT_SHOP_PICTURE = "https://res.cloudinary.com/APP_ID/PATH/upload/VERSION/*.png"
}