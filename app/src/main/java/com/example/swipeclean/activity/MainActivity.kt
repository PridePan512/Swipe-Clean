package com.example.swipeclean.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.swipeclean.R
import com.example.swipeclean.adapter.SwipeCleanAlbumAdapter
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.business.SwipeCleanConfigHost
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Constants.KEY_INTENT_ALBUM_ID
import com.example.swipeclean.model.Constants.MIN_SHOW_LOADING_TIME
import com.example.swipeclean.utils.ReClickPreventViewClickListener
import com.example.swipeclean.utils.StringUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mContentView: View
    private lateinit var mAlbumsView: View
    private lateinit var mEmptyView: View
    private lateinit var mCompletedTextView: TextView
    private lateinit var mCleanedTextView: TextView
    private lateinit var mSortOrderTextView: TextView
    private lateinit var mSortOrderView: View
    private lateinit var mAdapter: SwipeCleanAlbumAdapter
    private lateinit var mLoadingView: View
    private var mSortOrderMode = SortOrderMode.DATE
    private val mSizeComparator: Comparator<Album> =
        Comparator.comparingInt(Album::getTotalCount).reversed()
    private val mDateComparator: Comparator<Album> =
        Comparator.comparingLong(Album::getDateTime).reversed()

    enum class SortOrderMode {
        SIZE,
        DATE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()

        // TODO: 在有权限时才进行同步
//        lifecycleScope.launch(Dispatchers.IO) {
//            AlbumController.getInstance(this@MainActivity).syncDatabase()
//        }
    }

    override fun onStart() {
        super.onStart()
        prepareData()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (mLoadingView.isVisible) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun initView() {
        mRecyclerView = findViewById(R.id.v_recyclerview)
        mContentView = findViewById(R.id.v_content)
        mEmptyView = findViewById(R.id.v_empty)
        mCompletedTextView = findViewById(R.id.tv_completed_content)
        mCleanedTextView = findViewById(R.id.tv_cleaned_content)
        mSortOrderTextView = findViewById(R.id.tv_sort_order)
        mSortOrderView = findViewById(R.id.v_sort_order)
        mAlbumsView = findViewById(R.id.v_albums)
        mLoadingView = findViewById(R.id.v_loading)

        mAdapter = SwipeCleanAlbumAdapter(object : SwipeCleanAlbumAdapter.ItemClickListener {
            override fun onCompletedItemClick(albumId: Long, albumFormatDate: String?) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(albumFormatDate)
                    .setMessage("要再次清理此文件夹中的图片吗")
                    .setCancelable(false)
                    .setNegativeButton("取消") { dialog, which -> }
                    .setPositiveButton("确认") { dialog, which ->
                        val album: Album =
                            AlbumController.getInstance(this@MainActivity).albums.stream()
                                .filter { item -> item.id == albumId }
                                .findFirst().orElse(null)
                        if (album.photos.isNotEmpty()) {
                            lifecycleScope.launch {
                                mLoadingView.visibility = View.VISIBLE
                                val startTime = SystemClock.elapsedRealtime()

                                withContext(Dispatchers.IO) {
                                    for (photo in album.photos) {
                                        photo.isDelete = false
                                        photo.isKeep = false
                                        AlbumController.getInstance(this@MainActivity)
                                            .cleanCompletedPhoto(photo)
                                    }

                                    runOnUiThread {
                                        val intent = Intent(
                                            this@MainActivity,
                                            OperationActivity::class.java
                                        )
                                        intent.putExtra(
                                            KEY_INTENT_ALBUM_ID,
                                            albumId
                                        )
                                        val spendTime =
                                            SystemClock.elapsedRealtime() - startTime
                                        mRecyclerView.postDelayed(
                                            {
                                                mLoadingView.visibility = View.GONE
                                                startActivity(intent)
                                            },
                                            MIN_SHOW_LOADING_TIME - spendTime
                                        )
                                    }
                                }
                            }
                        }
                    }
                    .show()
            }

            override fun onUncompletedItemClick(albumId: Long) {
                val intent: Intent = if (isAlbumOperated(albumId)) {
                    Intent(
                        this@MainActivity,
                        RecycleBinActivity::class.java
                    )

                } else {
                    Intent(
                        this@MainActivity,
                        OperationActivity::class.java
                    )
                }
                intent.putExtra(
                    KEY_INTENT_ALBUM_ID,
                    albumId
                )
                startActivity(intent)
            }
        })
        mAdapter.setHasStableIds(true)

        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.setLayoutManager(LinearLayoutManager(this))
        mRecyclerView.setAdapter(mAdapter)
    }

    private fun loadAlbums() {
        mLoadingView.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val startTime = SystemClock.elapsedRealtime()
            val albums = AlbumController.getInstance(this@MainActivity).loadAlbums()

            runOnUiThread {
                val spendTime = SystemClock.elapsedRealtime() - startTime
                mRecyclerView.postDelayed(
                    {
                        mLoadingView.visibility = View.GONE
                        if (albums.isEmpty()) {
                            mEmptyView.visibility = View.VISIBLE
                            mAlbumsView.visibility = View.GONE
                            mSortOrderView.visibility = View.GONE

                        } else {
                            mSortOrderView.setOnClickListener(ReClickPreventViewClickListener.defendFor {
                                mSortOrderMode =
                                    if (mSortOrderMode == SortOrderMode.SIZE) SortOrderMode.DATE else SortOrderMode.SIZE
                                sortAlbums(albums)
                                mRecyclerView.scrollToPosition(0)
                            })

                            mSortOrderView.visibility = View.VISIBLE
                            mEmptyView.visibility = View.GONE
                            mAlbumsView.visibility = View.VISIBLE

                            mCleanedTextView.text =
                                StringUtils.getHumanFriendlyByteCount(
                                    SwipeCleanConfigHost.getCleanedSize(
                                        this@MainActivity
                                    ), 1
                                )

                            mCompletedTextView.text =
                                String.format(
                                    Locale.getDefault(),
                                    "%d/%d",
                                    albums.stream().filter(Album::isCompleted).count(),
                                    albums.size
                                )

                            sortAlbums(albums)
                        }
                    },
                    MIN_SHOW_LOADING_TIME - spendTime
                )
            }
        }
    }

    private fun prepareData() {
        if (Build.VERSION.SDK_INT < 30) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: 处理低版本的授权
            }

        } else {
            if (!Environment.isExternalStorageManager()
            ) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("请授权")
                    .setMessage("授权以访问设备上的图片")
                    .setCancelable(false)
                    .setNegativeButton("关闭") { dialog, which ->
                        finish()
                    }
                    .setPositiveButton("去授权") { dialog, which ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                            "package:${packageName}".toUri()
                        )
                        intent.setComponent(
                            ComponentName(
                                "com.android.settings",
                                "com.android.settings.Settings\$AppManageExternalStorageActivity"
                            )
                        )

                        if (packageManager.resolveActivity(
                                intent,
                                PackageManager.MATCH_DEFAULT_ONLY
                            ) != null
                        ) {
                            startActivity(intent)

                        } else {
                            val intent =
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    "package:${packageName}".toUri()
                                )
                            intent.addCategory(Intent.CATEGORY_DEFAULT)
                            startActivity(intent)
                        }
                    }
                    .show()

            } else {
                loadAlbums()
            }
        }
    }

    private fun sortAlbums(albums: List<Album>) {
        mSortOrderTextView.text = if (mSortOrderMode == SortOrderMode.SIZE) "大小" else "日期"
        Collections.sort(
            albums,
            if (mSortOrderMode == SortOrderMode.SIZE) mSizeComparator else mDateComparator
        )
        mAdapter.data = albums
    }

    fun isAlbumOperated(albumId: Long): Boolean {
        val album: Album =
            AlbumController.getInstance(this).albums.stream().filter { item -> item.id == albumId }
                .findFirst().orElse(null)
        if (album.photos == null || album.photos.isEmpty()) {
            return true
        }

        return album.isOperated
    }
}