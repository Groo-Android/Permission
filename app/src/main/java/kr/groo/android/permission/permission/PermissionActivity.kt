package kr.groo.android.permission.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kr.groo.android.permission.R
import java.util.ArrayList

/**
 * 앱 권한 요청 세부 로직을 수행하는 역할
 *
 * 대략적인 퍼미션 로직 시나리오 (각종 예외에 따라 분기 처리 됨)
 * 1) checkPermissionAlreadyGranted: 사전에 권한이 허용된지 확인
 * 2) showRationaleDialog: 권한이 필요한 이유를 설명
 * 3) showRequestPermissionDialog: OS 자체 권한 요청
 * 4) showStartSettingDialog: 접근권한 설정 화면 이동 유도
 */
class PermissionActivity : AppCompatActivity() {

    private var permissions: ArrayList<String>? = null
    private var rationaleMessage: String? = null
    private var settingMessage: String? = null

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var startSettingLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val EXTRA_PERMISSIONS = "EXTRA_PERMISSIONS"
        private const val EXTRA_RATIONALE_MESSAGE = "EXTRA_RATIONALE_MESSAGE"
        private const val EXTRA_SETTING_MESSAGE = "EXTRA_SETTING_MESSAGE"

        private var grantedListener: (() -> Unit)? = null
        private var deniedListener: ((List<String>?) -> Unit)? = null

        fun startActivity(
            context: Context,
            permissions: ArrayList<String>? = null,
            rationaleMessage: String? = null,
            settingMessage: String? = null
        ) {
            Log.d("PermissionActivity", "[Call] startActivity [Value] permissions: $permissions | rationaleMessage: $rationaleMessage | settingMessage: $settingMessage")

            val intent = Intent(context, PermissionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.putStringArrayListExtra(EXTRA_PERMISSIONS, permissions)
            intent.putExtra(EXTRA_RATIONALE_MESSAGE, rationaleMessage)
            intent.putExtra(EXTRA_SETTING_MESSAGE, settingMessage)
            context.startActivity(intent)
        }

        fun setGrantedListener(listener: (() -> Unit)) {
            Log.d("PermissionActivity","[Call] setGrantedListener")
            grantedListener = listener
        }

        fun setDeniedListener(listener: ((List<String>?) -> Unit)) {
            Log.d("PermissionActivity","[Call] setDeniedListener")
            deniedListener = listener
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.apply {
            permissions = getStringArrayListExtra(EXTRA_PERMISSIONS)
            rationaleMessage = getStringExtra(EXTRA_RATIONALE_MESSAGE)
            settingMessage = getStringExtra(EXTRA_SETTING_MESSAGE)

            Log.d("PermissionActivity","[Call] intent.apply [Value] permissions: $permissions | rationaleMessage: $rationaleMessage | settingMessage: $settingMessage")
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionMap ->
            Log.d("PermissionActivity", "[Call] requestPermissionLauncher [Value] permissionMap: $permissionMap")

            val deniedPermissions = permissionMap
                .filter { it.value.not() }
                .map { it.key }

            if (deniedPermissions.isEmpty()) {
                Log.d("PermissionActivity", "[Condition] deniedPermissions.isEmpty()")
                permissionGranted()
            } else {
                Log.d("PermissionActivity", "[Condition] deniedPermissions.isNotEmpty [Value] deniedPermissions: $deniedPermissions")
                showStartSettingDialog(deniedPermissions)
            }
        }

        startSettingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d("PermissionActivity", "[Call] startSettingLauncher")
            checkPermissionAlreadyGranted(true)
        }

        checkPermissionAlreadyGranted(false)
    }

    /**
     * 퍼미션 권한 허용
     */
    private fun permissionGranted() {
        Log.d("PermissionActivity", "[Call] permissionGranted")

        grantedListener?.invoke()
        finish()
        overridePendingTransition(0, 0)
    }

    /**
     * 퍼미션 권한 거부
     */
    private fun permissionDenied(deniedPermissions: List<String>?) {
        Log.d("PermissionActivity", "[Call] permissionDenied [Value] deniedPermissions: $deniedPermissions")

        deniedListener?.invoke(deniedPermissions)
        finish()
        overridePendingTransition(0, 0)
    }

    /**
     * 앱 접근권한 설정 화면에서도 권한을 거부했다면 더이상 분기 처리를 진행하지 않음
     */
    private fun checkPermissionAlreadyGranted(isFromActivityResult: Boolean) {
        Log.d("PermissionActivity", "[Call] checkPermissionAlreadyGranted [Value] isFromActivityResult: $isFromActivityResult")

        val requestPermissions = permissions
            ?.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

        when {
            requestPermissions.isNullOrEmpty() -> permissionGranted()
            isFromActivityResult -> permissionDenied(requestPermissions)
            rationaleMessage != null -> showRationaleDialog(requestPermissions)
            else -> showRequestPermissionDialog(requestPermissions)
        }
    }

    /**
     * 해당 퍼미션을 필요로 하는 이유를 설명 (구글에서는 해당 스탭을 지향하나 기존 여기어때 퍼미션 시나리오에서는 생략됨)
     */
    private fun showRationaleDialog(requestPermissions: List<String>?) {
        Log.d("PermissionActivity", "[Call] showRationaleDialog [Value] requestPermissions: $requestPermissions")

        AlertDialog.Builder(this)
            .setTitle(rationaleMessage)
            .setPositiveButton(getString(R.string.dialog_ok)) { _, _ ->
                Log.d("PermissionActivity","[Event] setPositiveButton")
                showRequestPermissionDialog(requestPermissions)
            }
            .show()
    }

    /**
     * OS 자체 퍼미션 요청 다이어로그 실행
     */
    private fun showRequestPermissionDialog(requestPermissions: List<String>?) {
        Log.d("PermissionActivity", "[Call] showRequestPermissionDialog [Value] requestPermissions: $requestPermissions")
        requestPermissionLauncher.launch(requestPermissions?.toTypedArray())
    }

    /**
     * 퍼미션 요청을 거부했을 때 앱 접근권한 설정 화면에서 한번 더 설정할 수 있도록 유도 (구글에서는 해당 스탭이 존재하지 않으나 기존 여기어때 퍼미션 시나리오에서는 존재)
     */
    private fun showStartSettingDialog(deniedPermissions: List<String>?) {
        Log.d("PermissionActivity", "[Call] showStartSettingDialog [Value] deniedPermissions: $deniedPermissions")

        if (settingMessage.isNullOrEmpty()) {
            permissionDenied(deniedPermissions)
            return
        }

        AlertDialog.Builder(this)
            .setTitle(settingMessage)
            .setPositiveButton(getString(R.string.dialog_setting)) { _, _ ->
                Log.d("PermissionActivity","[Event] setPositiveButton")
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                startSettingLauncher.launch(intent)
            }
            .setNegativeButton(getString(R.string.dialog_cancel)) { _, _ ->
                Log.d("PermissionActivity","[Event] setNegativeButton")
                permissionDenied(deniedPermissions)
            }
            .show()
    }
}