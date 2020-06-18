package uk.co.joshuabutton.wellbeingleaderboard

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.util.Patterns
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_sign_up.*


class SignUpActivity : AppCompatActivity() {

    private fun AppCompatActivity.hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        // else {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        // }
    }

    public var globalLBList = emptyList<User>()

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        auth = FirebaseAuth.getInstance()

        btn_sign_up.setOnClickListener {
            signUpUser()
        }


    }

    private fun signUpUser() {
        if (tv_name.text.toString().isEmpty()) {
            tv_name.error = "Please enter a username"
            tv_name.requestFocus()
            return
        }


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

        hideKeyboard()

        auth.createUserWithEmailAndPassword(tv_username.text.toString(), tv_password.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveUserToDatabase()
                } else {
                    Toast.makeText(
                        baseContext, "Sign Up failed. This email may be already taken.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun saveUserToDatabase() {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        var login = LoginActivity()

        val user = User(uid, tv_name.text.toString(), 0, login.currentLoginCity, login.currentLoginCountry, listOf(tv_name.text.toString()))

        ref.setValue(user)
            .addOnSuccessListener {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }.addOnFailureListener {
                Toast.makeText(
                    baseContext, "Failed to write user data to database.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


}


class User(
    val uid: String,
    val username: String,
    val totalInAppTime: Long,
    val userCity: String,
    val userCountry: String,
    val friendsList: List<String>
) {
    constructor() : this("", "", 0, "", "", emptyList<String>())
}