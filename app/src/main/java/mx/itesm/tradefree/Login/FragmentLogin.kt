package mx.itesm.tradefree.Login

import android.content.Intent
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.fragment_login.*
import mx.itesm.tradefree.BaseFragment
import mx.itesm.tradefree.Home.ActivityHome
import mx.itesm.tradefree.R
import mx.itesm.tradefree.Register.ActivityRegister


class FragmentLogin : BaseFragment(), View.OnClickListener {

    private lateinit var viewModelLogin: ViewModelLogin
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModelLogin = ViewModelProviders.of(this).get(ViewModelLogin::class.java)
        val root = inflater.inflate(R.layout.fragment_login, container, false)

        // Buttons Listeners
        val btnRegister: Button = root.findViewById(R.id.btnRegistrate)
        val btnLogin: Button = root.findViewById(R.id.btnLogin)
        val btnGoogle: SignInButton = root.findViewById(R.id.btnGoogle)
        btnRegister.setOnClickListener(this)
        btnLogin.setOnClickListener(this)
        btnGoogle.setOnClickListener(this)

        // Google Sign In
        googleSignIn()

        // Initialize Firebase Auth
        firebaseInit()

        return root

    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        Log.d("AUTH", currentUser?.email.toString())
    }

    override fun onResume() {
        showProgressDialog()
        if (auth.currentUser != null) {
            val intent = Intent(activity, ActivityHome::class.java)
            startActivity(intent)
        }
        hideProgressDialog()
        super.onResume()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnRegistrate -> signUpWithEmailPassword()
            R.id.btnLogin -> signInWithEmailPassword(inputEmailLogin.text.toString(), inputPasswordLogin.text.toString())
            R.id.btnGoogle -> signInWithGoogle()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)

            }
        }
    }

    /**
     *  Firebase initialization.
     */
    private fun firebaseInit() {
        auth = FirebaseAuth.getInstance()
    }

    /**
     *  Google signin configuration.
     */
    private fun googleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    /**
     *  This method authenticates the user with google credentials.
     */
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val intent = Intent(context, ActivityHome::class.java)
                    startActivity(intent)
                    activity?.finishAffinity()
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                } else {
                    Log.w(TAG, "signInWithCredential:failure", it.exception)
                    Toast.makeText(activity, LOGIN_ERROR,Toast.LENGTH_LONG).show()
                }
                hideProgressDialog()
            }
    }

    /**
     * This method authenticates the user with his email and password.
     *
     * @param email     the user email retrieved from inputEmailLogin
     * @param password  the user password retrieved from inputPasswordlLogin
     * @return          if the user fill blank values
     */
    private fun signInWithEmailPassword(email: String, password: String) {
        view?.let { hideKeyboard(it) }
        if (!validateForm()) {
            Toast.makeText(activity, LOGIN_ERROR, Toast.LENGTH_LONG).show()
            return
        }
        showProgressDialog()
        Log.d(TAG, email + password)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val intent = Intent(context, ActivityHome::class.java)
                    startActivity(intent)
                    activity?.finishAffinity()
                } else {
                    inputEmailLogin.text.clear()
                    inputPasswordLogin.text.clear()
                    Toast.makeText(activity, LOGIN_ERROR, Toast.LENGTH_LONG).show()
                }
                hideProgressDialog()
            }
    }

    /**
     * This method creates user registration activity.
     */
    private fun signUpWithEmailPassword() {
        showProgressDialog()
        val intent = Intent(context, ActivityRegister::class.java)
        startActivity(intent)
        hideProgressDialog()
    }

    /**
     * This method sign the user with google.
     */
    private fun signInWithGoogle() {
        showProgressDialog()
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    /**
     * Validates if the email and password inputs are not blank.
     *
     * @return  If the user correctly fill their email and password.
     */
    private fun validateForm(): Boolean {
        var valid = true
        val email = inputEmailLogin.text.toString()
        val password = inputPasswordLogin.text.toString()
        if (email.isEmpty()) valid = false
        if (password.isEmpty()) valid = false
        return valid
    }

    companion object {
        private const val TAG = "LOGIN_FRAGMENT"
        private const val RC_SIGN_IN = 9001
        private const val LOGIN_ERROR = "Ingrese correctamente su correo y/o contraseña."
    }

}
