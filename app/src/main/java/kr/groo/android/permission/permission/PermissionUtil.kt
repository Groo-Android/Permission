package kr.groo.android.permission.permission

import android.Manifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 퍼미션 체크 및 요청시 사용하는 모델 클래스
 */
sealed class PermissionKind(
    open val permissions: ArrayList<String>? = null,
    open val rationaleMessage: String? = null,
    open val settingMessage: String? = null
) {
    // 단말 정보 조회 (OS 10 미만)
    data class PhoneState(
        override val permissions: ArrayList<String>? = arrayListOf(Manifest.permission.READ_PHONE_STATE),
        override val rationaleMessage: String? = null,
        override val settingMessage: String? = null
    ) : PermissionKind()

    // 전화 걸기
    data class CallPhone(
        override val permissions: ArrayList<String>? = arrayListOf(Manifest.permission.CALL_PHONE),
        override val rationaleMessage: String? = null,
        override val settingMessage: String? = null
    ) : PermissionKind()

    // 위치 정보 조회
    data class Location(
        override val permissions: ArrayList<String>? = arrayListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        override val rationaleMessage: String? = null,
        override val settingMessage: String? = null
    ) : PermissionKind()

    // 저장소 접근
    data class Storage(
        override val permissions: ArrayList<String>? = arrayListOf(Manifest.permission.READ_EXTERNAL_STORAGE),
        override val rationaleMessage: String? = null,
        override val settingMessage: String? = null
    ) : PermissionKind()

    // 카메라 사용
    data class Camera(
        override val permissions: ArrayList<String>? = arrayListOf(Manifest.permission.CAMERA),
        override val rationaleMessage: String? = null,
        override val settingMessage: String? = null
    ) : PermissionKind()
}

/**
 * 퍼미션 요청 응답 결과 모델 클래스
 */
sealed class PermissionResult {
    data class Granted(val permissionKind: PermissionKind) : PermissionResult()
    data class Denied(val permissionKind: PermissionKind, val deniedPermissions: List<String>?) : PermissionResult()
}

class PermissionResultState {
    var onGranted: ((permissionKind: PermissionKind) -> Unit)? = null
    var onDenied: ((permissionKind: PermissionKind, deniedPermissions: List<String>?) -> Unit)? = null
}

fun Flow<PermissionResult>.onEachPermissionResultState(block: PermissionResultState.() -> Unit) = onEach {
    val response = PermissionResultState().apply(block)
    when (it) {
        is PermissionResult.Granted -> response.onGranted?.invoke(it.permissionKind)
        is PermissionResult.Denied -> response.onDenied?.invoke(it.permissionKind, it.deniedPermissions)
    }
}

fun Flow<PermissionResult>.permissionLaunchIn(scope: CoroutineScope, block: PermissionResultState.() -> Unit) {
    onEachPermissionResultState(block).launchIn(scope)
}