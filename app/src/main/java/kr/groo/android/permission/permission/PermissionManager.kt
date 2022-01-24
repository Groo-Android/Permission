package kr.groo.android.permission.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * 외부에서 퍼미션 기능을 간편히 사용할 수 있도록 돕는 역할
 */
@ExperimentalCoroutinesApi
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        @RequiresApi(Build.VERSION_CODES.Q)
        private const val ACCESS_MEDIA_LOCATION = Manifest.permission.ACCESS_MEDIA_LOCATION
        private const val WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    /**
     * 퍼미션 요청 플로우 없이 현재 퍼미션 상태만을 간단 체크 후 반환
     */
    fun checkPermission(permissionKind: PermissionKind): Boolean {
        quarterOsVersion(permissionKind)

        val deniedPermissions = permissionKind.permissions
            ?.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }

        return deniedPermissions?.isEmpty() == true
    }

    /**
     * 퍼미션 요청 플로우 진행
     */
    fun requestPermission(permissionKind: PermissionKind): Flow<PermissionResult> {
        quarterOsVersion(permissionKind)
        return startPermissionActivity(permissionKind)
    }

    /**
     * 퍼미션 전체 요청 플로우 진행
     *
     * PhoneState 퍼미션은 OS 10 미만에서만 요청되어야 하므로 전체 요청 플로우에 포함 시키지 않음
     * 또한, 전체 요청 플로우에서 SettingDialog가 포함되면 사용자 인터렉션이 너무 복잡해지므로 전체 요청시에는 이를 제외함
     */
    fun requestAllPermissions(): Flow<Pair<PermissionResult, Boolean>> = callbackFlow {
        val allPermissions = arrayListOf(
            PermissionKind.CallPhone(settingMessage = null),
            PermissionKind.Location(settingMessage = null),
            PermissionKind.Storage(),
            PermissionKind.Camera()
        )

        allPermissions.forEachIndexed { index, permissionKind ->
            if (permissionKind is PermissionKind.Storage) quarterOsVersion(permissionKind)

            startPermissionActivity(permissionKind).collect { result ->
                val isLastIdx = index == allPermissions.size - 1

                trySend(Pair(result, isLastIdx))
                if (isLastIdx) close()
            }
        }

        awaitClose()
    }

    private fun quarterOsVersion(permissionKind: PermissionKind) {
        if (permissionKind is PermissionKind.Storage) {
            Log.d("PermissionManager", "[Condition] permissionKind is Storage")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && permissionKind.permissions?.contains(WRITE_EXTERNAL_STORAGE) == false)
                permissionKind.permissions?.add(WRITE_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && permissionKind.permissions?.contains(ACCESS_MEDIA_LOCATION) == false)
                permissionKind.permissions?.add(ACCESS_MEDIA_LOCATION)
        }
    }

    private fun startPermissionActivity(permissionKind: PermissionKind): Flow<PermissionResult> = callbackFlow {
        Log.d("PermissionManager", "[Call] startPermissionActivity [Value] permissionKind: $permissionKind")
        PermissionActivity.apply {
            startActivity(
                context = context,
                permissions = permissionKind.permissions,
                rationaleMessage = permissionKind.rationaleMessage,
                settingMessage = permissionKind.settingMessage
            )
            setGrantedListener {
                trySend(PermissionResult.Granted(permissionKind))
                close()
            }
            setDeniedListener {
                trySend(PermissionResult.Denied(permissionKind, it))
                close()
            }
        }
        awaitClose()
    }
}