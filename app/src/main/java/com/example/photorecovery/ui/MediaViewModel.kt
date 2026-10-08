package com.example.photorecovery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.photorecovery.data.DeepRootScanner
import com.example.photorecovery.data.MediaItem
import com.example.photorecovery.data.MediaScanner
import com.example.photorecovery.data.RecoveryRepository
import com.example.photorecovery.data.RecoverySource
import kotlinx.coroutines.launch

/**
 * 扫描与恢复的共享 ViewModel。
 * 扫描在后台协程执行，结果以 LiveData 暴露给界面。
 */
class MediaViewModel(app: Application) : AndroidViewModel(app) {

    private val _items = MutableLiveData<List<MediaItem>>(emptyList())
    val items: LiveData<List<MediaItem>> = _items

    private val _scanning = MutableLiveData(false)
    val scanning: LiveData<Boolean> = _scanning

    private val _selectedKeys = MutableLiveData<Set<String>>(emptySet())
    val selectedKeys: LiveData<Set<String>> = _selectedKeys

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    val selectedCount: Int get() = _selectedKeys.value?.size ?: 0

    fun startQuickScan() {
        _scanning.value = true
        _error.value = null
        viewModelScope.launch {
            val items = MediaScanner.scanRecycleBin(getApplication<Application>().contentResolver)
            _items.value = items
            _selectedKeys.value = emptySet()
            _scanning.value = false
        }
    }

    fun startDeepScan() {
        _scanning.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val items = DeepRootScanner.scanDeep()
                _items.value = items
                _selectedKeys.value = emptySet()
            } catch (e: IllegalStateException) {
                _error.value = "ROOT_NOT_AVAILABLE"
                _items.value = emptyList()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _scanning.value = false
            }
        }
    }

    fun toggleSelect(key: String) {
        val current = _selectedKeys.value.orEmpty().toMutableSet()
        if (!current.add(key)) current.remove(key)
        _selectedKeys.value = current
    }

    /** 全选/取消全选传入的一组 key。 */
    fun selectAll(keys: Set<String>) {
        _selectedKeys.value = if (_selectedKeys.value == keys) emptySet() else keys
    }

    /** 恢复所有选中的项目，完成后回调 (成功数, 错误消息)。 */
    fun restoreSelected(onDone: (Int, String?) -> Unit) {
        val items = _items.value.orEmpty().filter { _selectedKeys.value.orEmpty().contains(it.key) }
        if (items.isEmpty()) return

        viewModelScope.launch {
            var ok = 0
            var firstError: String? = null
            for (item in items) {
                val result = when (item.source) {
                    RecoverySource.TRASH -> RecoveryRepository.restoreTrashItem(getApplication(), item)
                    RecoverySource.DEEP -> RecoveryRepository.restoreDeepItem(getApplication(), item)
                }
                if (result.isSuccess) ok++
                else if (firstError == null) firstError = result.exceptionOrNull()?.message
            }
            onDone(ok, firstError)

            // 刷新列表：移除已恢复的项目，清空选中
            _items.value = _items.value.orEmpty().filterNot { _selectedKeys.value.orEmpty().contains(it.key) }
            _selectedKeys.value = emptySet()
        }
    }
}
