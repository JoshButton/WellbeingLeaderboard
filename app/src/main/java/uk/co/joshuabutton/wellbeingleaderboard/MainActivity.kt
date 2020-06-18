package uk.co.joshuabutton.wellbeingleaderboard

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.home.*
import kotlinx.android.synthetic.main.nav_header.*
import uk.co.joshuabutton.wellbeingleader.userItem
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        var currentUser: User? = null
        var thisUser: User? = null
    }

    var globalLBList: List<userItem> = emptyList()
    var localLBList: List<userItem> = emptyList()
    var friendsLBList: List<userItem> = emptyList()

    lateinit var globalFragment: GlobalFragment
    lateinit var localFragment: LocalFragment
    lateinit var friendsFragment: FriendsFragment


    private lateinit var tvUsageStats: TextView

    lateinit var locationManager: LocationManager
    private var hasGPS = false
    private var hasNetwork = false
    private var locationGPS: Location? = null
    private var locationNetwork: Location? = null

    lateinit var toggle: ActionBarDrawerToggle
    private lateinit var bottomNav: BottomNavigationView
    private var totalTime: Long = 0

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        tvUsageStats = findViewById(R.id.tvUsageStats)

        if (checkUsageStatsPermission()) {
            showUsageStats()
        } else {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        bottomNav = findViewById(R.id.bottomNavigationView)

        loadFrags()
        getLeaderboards()
    }

    private fun loadFrags() {
        globalFragment = GlobalFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentHolder, globalFragment, "globalBoardFrag")
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()

        bottomNav.setOnNavigationItemSelectedListener {
            when (it.itemId) {

                R.id.globalTab -> {
                    globalFragment = GlobalFragment()
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragmentHolder, globalFragment, "globalBoardFrag")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
                R.id.friendsTab -> {
                    friendsFragment = FriendsFragment()
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragmentHolder, friendsFragment, "friendsBoardFrag")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
                R.id.countryTab -> {
                    localFragment = LocalFragment()
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragmentHolder, localFragment, "localBoardFrag")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
            }
            true
        }

        fetchCurrentUser()

        toggle = ActionBarDrawerToggle(this, drawer_layout, R.string.open, R.string.close)
        drawer_layout.addDrawerListener(toggle)

        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nav_view.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_logout -> logoutNow()
                R.id.btnSyncGPS -> getLocation()
                R.id.nav_add_friends -> addFriendBox()
            }
            true
        }
    }

    private fun reloadFrags() {
        globalFragment = GlobalFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentHolder, globalFragment, "globalBoardFrag")
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    private fun getLeaderboards() {
        globalLBList = emptyList()
        localLBList = emptyList()
        friendsLBList = emptyList()
        val ref = FirebaseDatabase.getInstance().getReference("/users/")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {}
            override fun onDataChange(p0: DataSnapshot) {
                for (x in p0.children) {
                    thisUser = x.getValue(User::class.java)
                    globalLBList += userItem(
                        thisUser!!.username, thisUser!!.userCountry, thisUser!!.totalInAppTime,
                        formatAppTimePretty(thisUser!!.totalInAppTime), thisUser!!.uid
                    )
                }
                Log.d("GLOBAL LEADERBOARD", globalLBList.toString())
                localLBList = globalLBList.filter { x -> x.country == currentUser?.userCountry }
                Log.d("LOCAL LEADERBOARD", localLBList.toString())
                friendsLBList =
                    globalLBList.filter { x -> currentUser?.friendsList!!.contains(x.name) }
                Log.d("FRIENDS LEADERBOARD", localLBList.toString())
            }
        })
        reloadFrags()
    }

    private fun formatAppTimePretty(input: Long): String {
        return String.format(
            "%02dH %02dm",
            TimeUnit.MILLISECONDS.toHours(input),
            TimeUnit.MILLISECONDS.toMinutes(input) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(input))
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        hasGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (hasGPS || hasNetwork) {

            if (hasGPS) {
                Toast.makeText(
                    baseContext,
                    "Has GPS",
                    Toast.LENGTH_SHORT
                ).show()
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000,
                    0F,
                    object : LocationListener {
                        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                        override fun onProviderEnabled(p0: String?) {}
                        override fun onProviderDisabled(p0: String?) {}
                        override fun onLocationChanged(location: Location?) {
                            if (location != null) {
                                locationGPS = location
                            }
                            locationManager.removeUpdates(this)
                        }
                    })
                val localGPpsLocation =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (localGPpsLocation != null)
                    locationGPS = localGPpsLocation
            }
            if (hasNetwork) {
                Toast.makeText(
                    baseContext,
                    "Has Network",
                    Toast.LENGTH_SHORT
                ).show()
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    0F,
                    object : LocationListener {
                        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                        override fun onProviderEnabled(p0: String?) {}
                        override fun onProviderDisabled(p0: String?) {}
                        override fun onLocationChanged(location: Location?) {
                            if (location != null) {
                                locationNetwork = location
                            }
                            locationManager.removeUpdates(this)
                        }
                    })
                val localNetworkLocation =
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (localNetworkLocation != null)
                    locationNetwork = localNetworkLocation
            }
            val geocoder = Geocoder(this)
            val strongestLocation: Location
            if (locationGPS != null && locationNetwork != null) {
                if (locationGPS!!.accuracy > locationNetwork!!.accuracy) {
                    strongestLocation = locationNetwork!!
                } else {
                    strongestLocation = locationGPS!!
                }
                try {
                    val addresses = geocoder.getFromLocation(
                        strongestLocation.latitude,
                        strongestLocation.longitude,
                        1
                    )
                    Toast.makeText(
                        baseContext,
                        "Latitude: " + strongestLocation.latitude + " LongitudeL " + strongestLocation.longitude,
                        Toast.LENGTH_SHORT
                    ).show()
                    var currentCity = ""
                    if (addresses[0].locality == null) {
                        currentCity = addresses[0].subAdminArea
                    } else {
                        currentCity = addresses[0].locality
                    }
                    var currentCountry = addresses[0].countryName

                    Toast.makeText(
                        baseContext,
                        "Location updated:\nCity: " + currentCity + " Country: " + currentCountry,
                        Toast.LENGTH_SHORT
                    ).show()

                    if (currentCity != null && currentCountry != null) {
                        cityText.text = currentCity
                        countryText.text = currentCountry

                        val uid = FirebaseAuth.getInstance().uid
                        val userRef = FirebaseDatabase.getInstance()
                            .getReference("/users/$uid")
                        var map = mutableMapOf<String, String>()
                        map["userCity"] = currentCity
                        userRef.updateChildren(map as Map<String, Any>)
                        map["userCountry"] = currentCountry
                        userRef.updateChildren(map as Map<String, Any>)
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Failed to get location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: IOException) {

                    when {
                        e.message == "grpc failed" -> {
                            Toast.makeText(
                                baseContext,
                                "getLocation failed, please try again later",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> throw e
                    }

                }
                fetchCurrentUser()
                getLeaderboards()
            }
        }
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                currentUser = p0.getValue(User::class.java)
                usernameText.text = currentUser?.username
                countryText.text = currentUser?.userCountry
                cityText.text = currentUser?.userCity
                tvUsageStats.text =
                    "APP TIME IN LAST 7 DAYS:\n" + formatAppTimePretty(currentUser!!.totalInAppTime)
                Toast.makeText(
                    baseContext,
                    "Score updated!",
                    Toast.LENGTH_SHORT
                ).show()
                reloadFrags()
            }

            override fun onCancelled(p0: DatabaseError) {}
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        showUsageStats()
        val uid = FirebaseAuth.getInstance().uid
        val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
        var map = mutableMapOf<String, Long>()
        map["totalInAppTime"] = totalTime
        userRef.updateChildren(map as Map<String, Any>)
    }

    fun addFriendBox() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Friends Manager")
        val dialogLayout = inflater.inflate(R.layout.add_friend_dialog, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.editText)
        builder.setView(dialogLayout)
        var currentFriendList = currentUser?.friendsList!!
        builder.setNegativeButton("REMOVE") { _, _ ->
            if (currentFriendList.any { x -> x == editText.text.toString() }) {
                currentFriendList = currentFriendList.filter { x -> x != editText.text.toString() }
                val uid = FirebaseAuth.getInstance().uid
                val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
                var map = mutableMapOf<String, Any>()
                map["friendsList"] = currentFriendList
                userRef.updateChildren(map as Map<String, Any>)
                Toast.makeText(
                    baseContext,
                    "Removed " + editText.text.toString() + " from your friends List",
                    Toast.LENGTH_SHORT
                ).show()

                fetchCurrentUser()
                getLeaderboards()
            } else if (currentUser!!.username == editText.text.toString()) {
                Toast.makeText(
                    baseContext,
                    "You can't remove yourself!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        builder.setPositiveButton("ADD") { _, _ ->
            if (currentFriendList.contains(editText.text.toString())) {
                Toast.makeText(
                    baseContext,
                    editText.text.toString() + " is already your friend",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (currentUser!!.username == editText.text.toString()) {
                Toast.makeText(
                    baseContext,
                    "You can't add yourself as a friend!",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (globalLBList.any { x -> x.name == editText.text.toString() }) {
                var newFriend = editText.text.toString()
                val uid = FirebaseAuth.getInstance().uid
                val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
                currentFriendList += newFriend
                var map = mutableMapOf<String, Any>()
                map["friendsList"] = currentFriendList
                userRef.updateChildren(map as Map<String, Any>)
                Toast.makeText(
                    baseContext,
                    "New friend added: $newFriend",
                    Toast.LENGTH_SHORT
                ).show()
                fetchCurrentUser()
                getLeaderboards()
            } else {
                Toast.makeText(
                    baseContext,
                    "Sorry user does not exist",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        builder.show()
    }

    private fun showUsageStats() {
        var usageStatsManager: UsageStatsManager =
            getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var cal: Calendar = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -7)
        var queryUsageStats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY
            , cal.timeInMillis, System.currentTimeMillis()
        )
        totalTime = 0
        for (i in 0 until queryUsageStats.size) {
            totalTime += queryUsageStats[i].totalTimeInForeground.toLong()
        }
        tvUsageStats.text = "APP TIME IN LAST 7 DAYS: \n" + formatAppTimePretty(totalTime)
    }

    private fun logoutNow() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun checkUsageStatsPermission(): Boolean {
        var appOpsManager: AppOpsManager?
        var mode: Int
        appOpsManager = getSystemService(Context.APP_OPS_SERVICE)!! as AppOpsManager
        mode = appOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
        return mode == MODE_ALLOWED
    }
}
