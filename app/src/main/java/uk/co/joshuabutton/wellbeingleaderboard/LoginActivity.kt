package uk.co.joshuabutton.wellbeingleaderboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException


class LoginActivity : AppCompatActivity() {

  private lateinit var auth: FirebaseAuth


  lateinit var locationManager: LocationManager
  private var hasGPS = false
  public var currentLoginCity = ""
  public var currentLoginCountry = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    if (checkUsageStatsPermission()) {
    } else {
      startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    when {
      ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
      ) == PackageManager.PERMISSION_GRANTED -> {
        Toast.makeText(
          baseContext, "LOCATION PERMISSION GRANTED",
          Toast.LENGTH_SHORT
        ).show()
      }
      else -> {
        // You can directly ask for the permission.
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),1)
      }
    }


    auth = FirebaseAuth.getInstance()

    btn_sign_up.setOnClickListener {
      getLocation()
      startActivity(Intent(this, SignUpActivity::class.java))
      finish()
    }

    btn_log_in.setOnClickListener {
      doLogin()
    }
  }

  private fun doLogin() {
    if (tv_username.text.toString().isEmpty()) {
      tv_username.error = "Please enter email"
      tv_username.requestFocus()
      return
    }

    if (!Patterns.EMAIL_ADDRESS.matcher(tv_username.text.toString()).matches()) {
      tv_username.error = "Please enter valid email"
      tv_username.requestFocus()
      return
    }

    if (tv_password.text.toString().isEmpty()) {
      tv_password.error = "Please enter password"
      tv_password.requestFocus()
      return
    }

    auth.signInWithEmailAndPassword(tv_username.text.toString(), tv_password.text.toString())
      .addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
          val user = auth.currentUser
          updateUI(user)
        } else {
          Toast.makeText(
            baseContext, "Login failed. Check email and password are correct. Do you have an account?",
            Toast.LENGTH_SHORT
          ).show()
          updateUI(null)
        }
      }
  }


  public fun checkUsageStatsPermission(): Boolean {
    var appOpsManager: AppOpsManager?
    var mode: Int
    appOpsManager = getSystemService(Context.APP_OPS_SERVICE)!! as AppOpsManager
    mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    return mode == AppOpsManager.MODE_ALLOWED
  }

  @SuppressLint("MissingPermission")
  private fun getLocation() {
    val geocoder = Geocoder(this)
    locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    hasGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    if (hasGPS) {
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
              try {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                currentLoginCity = if (addresses[0].locality == null) {
                  addresses[0].subAdminArea
                } else {
                  addresses[0].locality
                }
                currentLoginCountry = addresses[0].countryName

                if (currentLoginCity != null && currentLoginCountry != null) {}
                else {
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

            }
            locationManager.removeUpdates(this);
          }
        })

    }
  }


  public override fun onStart() {
    super.onStart()
    // Check if user is signed in (non-null) and update UI accordingly.
    val currentUser = auth.currentUser
    updateUI(currentUser)
  }

  fun updateUI(currentUser: FirebaseUser?) {

    if (currentUser != null) {
      startActivity(Intent(this, MainActivity::class.java))
        finish()
    } else {
    }
  }}

