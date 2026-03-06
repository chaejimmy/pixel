import com.shourov.apps.pacedream.model.response.User

interface PreferenceStorage {
    var authorization: String?
    var user: User?
    var deviceToken: String?
    var userMode: String?
    var isLoggedIn: Boolean?
    fun clear()
}

/**
 *  Throws an [IllegalStateException] if no user details is stored
 **/
val PreferenceStorage.userOrThrow: User
    get() = checkNotNull(user) {
        "User is not logged in."
    }

class PreferenceStorageImpl : PreferenceStorage{
    private var _authorization: String? = null
    private var _user: User? = null
    private var _deviceToken: String? = null
    private var _userMode: String? = null
    private var _isLoggedIn: Boolean? = null

    override var authorization: String?
        get() = _authorization
        set(value) { _authorization = value}
    override var user: User?
        get() = _user
        set(value) { _user = value}
    override var deviceToken: String?
        get() = _deviceToken
        set(value) { _deviceToken = value}
    override var userMode: String?
        get() = _userMode
        set(value) { _userMode = value}
    override var isLoggedIn: Boolean?
        get() = _isLoggedIn
        set(value) { _isLoggedIn = value}

    override fun clear() {
        _authorization = null
        _user = null
        _deviceToken = null
        _userMode = null
        _isLoggedIn = null
    }
}


// todo remember to add encryption when implementing this with data store
/*class AndroidPreferenceStorage(private val context: Context, moshi: Moshi) : PreferenceStorage {

    companion object {
        private const val PREFS_NAME = "Prefs"
        private const val PREF_AUTHORIZATION = "pref_authorization"
        private const val PREF_USER = "pref_user"
        private const val PREF_DEVICE_TOKEN = "pref_device_token"
        private const val PREF_USER_MODE = "pref_user_mode"
        private const val LOGGED_IN = "loggedIn"
    }



    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            PREF_USER -> {
                _userFlow.tryEmit(user)
            }
        }
    }

    private val prefs: Lazy<SharedPreferences> = lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ).apply {
            registerOnSharedPreferenceChangeListener(changeListener)
        }
    }

    override var authorization: String? by StringPreference(prefs, PREF_AUTHORIZATION, null)
    override var user: User? by UserPreference(prefs, moshi, PREF_USER, null)

    private val _userFlow: MutableStateFlow<User?> by lazy {
        MutableStateFlow(user)
    }
    override val userFlow: StateFlow<User?>
        get() = _userFlow
    override var deviceToken: String? by StringPreference(prefs, PREF_DEVICE_TOKEN, null)
    override var userMode: String? by StringPreference(
        prefs,
        PREF_USER_MODE,
        Constant.USER_MODE_GUEST
    )


    override var isLoggedIn: Boolean?
        get() = prefs.value.getBoolean(LOGGED_IN, false)
        set(value) {
            prefs.value.edit {
                if (value != null) {
                    putBoolean(LOGGED_IN, value)
                }
            }
        }


    override fun clear() {
        prefs.value.edit {
            clear()
        }
    }
}

private class StringPreference(
    private val preferences: Lazy<SharedPreferences>,
    private val name: String,
    private val defaultValue: String?
) : ReadWriteProperty<Any, String?> {

    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ): String? {
        return preferences.value.getString(name, defaultValue)
    }

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: String?
    ) {
        preferences.value.edit { putString(name, value) }
    }
}*/

/*private class UserPreference(
    private val preferences: Lazy<SharedPreferences>,
    moshi: Moshi,
    private val name: String,
    private val defaultValue: User?
) : ReadWriteProperty<Any, User?> {

    private val adapter = moshi.adapter(User::class.java)

    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ): User? {
        val json = preferences.value.getString(name, null) ?: return defaultValue
        try {
            return adapter.fromJson(json)
        } catch (t: Throwable) {
            Timber.e(t, "Failed to deserialize User json: $json")
        }
        return null
    }

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: User?
    ) {
        if (value == null) {
            preferences.value.edit { putString(name, null) }
        } else {
            try {
                val json = adapter.toJson(value)
                preferences.value.edit { putString(name, json) }
            } catch (t: Throwable) {
                Timber.e(t, "Failed to serialize User: $value")
            }
        }
    }
}*/

//private class LocationPreference(
//    private val preferences: Lazy<SharedPreferences>,
//    moshi: Moshi,
//    private var countryId: Int,
//    private val name: String
//) : ReadWriteProperty<Any, Location> {
//
//    companion object {
//        private val DEFAULT_LOCATION_MAP = mapOf(
//            Constants.COUNTRY_CODE_UAE to Location(
//                latitude = 25.2048,
//                longitude = 55.2708
//            ),
//            Constants.COUNTRY_CODE_OMAN to Location(
//                latitude = 23.58754,
//                longitude = 58.37502
//            ),
//            Constants.COUNTRY_CODE_INDIA to Location(
//                latitude = 12.9716,
//                longitude = 77.5946
//            )
//        )
//    }
//
//    private val adapter = moshi.adapter(Location::class.java)
//
//    override fun getValue(
//        thisRef: Any,
//        property: KProperty<*>
//    ): Location {
//        val json = preferences.value.getString(name, null)
//        if (json != null) {
//            try {
//                return checkNotNull(adapter.fromJson(json))
//            } catch (t: Throwable) {
//                Timber.e(t, "Failed to deserialize Location json: $json")
//            }
//        }
//
//        return DEFAULT_LOCATION_MAP[preferences.value.getInt("pref_country_id", 0)] ?: Location(
//            latitude = 25.2048,
//            longitude = 55.2708
//        )
//    }
//
//    override fun setValue(
//        thisRef: Any,
//        property: KProperty<*>,
//        value: Location
//    ) {
//        try {
//            val json = adapter.toJson(value)
//            preferences.value.edit { putString(name, json) }
//        } catch (t: Throwable) {
//            Timber.e(t, "Failed to serialize Location: $value")
//        }
//    }
//}
