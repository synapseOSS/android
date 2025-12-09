package com.synapse.social.studioasinc

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
// import androidx.appcompat.app.AppCompatActivity
import com.synapse.social.studioasinc.permissionreq.AskPermission

class CheckpermissionActivity : BaseActivity() {
    
    private lateinit var linear1: LinearLayout
    private var askPermission: AskPermission? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkpermission)
        initialize()
        initializeLogic()
    }
    
    private fun initialize() {
        linear1 = findViewById(R.id.linear1)
    }
    
    private fun initializeLogic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
        
        askPermission = AskPermission(this)
        askPermission?.checkAndRequestPermissions()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            AskPermission.PERMISSION_REQUEST_CODE -> {
                // Permission result is handled by the timer in AskPermission
                // This callback ensures the system knows we're handling the result
            }
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back navigation during permission check
    }
    
    override fun onDestroy() {
        super.onDestroy()
        askPermission?.cleanup()
    }
}
