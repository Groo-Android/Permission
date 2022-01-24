package kr.groo.android.permission.ui

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kr.groo.android.permission.R
import kr.groo.android.permission.databinding.ActivityMainBinding
import kr.groo.android.permission.permission.PermissionKind
import kr.groo.android.permission.permission.PermissionManager
import kr.groo.android.permission.permission.permissionLaunchIn
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.activity = this
        binding.lifecycleOwner = this
    }

    fun onClickPhoneState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionManager.requestPermission(PermissionKind.PhoneState(settingMessage = getString(R.string.setting_phone_state)))
                .permissionLaunchIn(lifecycleScope) {
                    onGranted = { toastGranted() }
                    onDenied = { _, _ ->
                        toastDenied()
                    }
                }
        } else {
            Toast.makeText(this, getString(R.string.toast_not_need), Toast.LENGTH_SHORT).show()
        }
    }

    fun onClickCallPhone() {
        permissionManager.requestPermission(PermissionKind.CallPhone(settingMessage = getString(R.string.setting_call_phone)))
            .permissionLaunchIn(lifecycleScope) {
                onGranted = { toastGranted() }
                onDenied = { _, _ ->
                    toastDenied()
                }
            }
    }

    fun onClickLocation() {
        permissionManager.requestPermission(PermissionKind.Location(settingMessage = getString(R.string.setting_location)))
            .permissionLaunchIn(lifecycleScope) {
                onGranted = { toastGranted() }
                onDenied = { _, _ ->
                    toastDenied()
                }
            }
    }

    fun onClickStorage() {
        permissionManager.requestPermission(PermissionKind.Storage(settingMessage = getString(R.string.setting_storage)))
            .permissionLaunchIn(lifecycleScope) {
                onGranted = { toastGranted() }
                onDenied = { _, _ ->
                    toastDenied()
                }
            }
    }

    fun onClickCamera() {
        permissionManager.requestPermission(PermissionKind.Camera(settingMessage = getString(R.string.setting_camera)))
            .permissionLaunchIn(lifecycleScope) {
                onGranted = { toastGranted() }
                onDenied = { _, _ ->
                    toastDenied()
                }
            }
    }

    private fun toastGranted() {
        Toast.makeText(this, getString(R.string.toast_granted), Toast.LENGTH_SHORT).show()
    }

    private fun toastDenied() {
        Toast.makeText(this, getString(R.string.toast_denied), Toast.LENGTH_SHORT).show()
    }
}