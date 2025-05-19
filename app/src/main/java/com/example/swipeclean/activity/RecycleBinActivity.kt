package com.example.swipeclean.activity

import android.content.res.Resources
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.swipeclean.R
import com.example.swipeclean.adapter.SwipeCleanRecycleBinAdapter
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.business.SwipeCleanConfigHost
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Photo
import com.example.swipeclean.utils.MediaStoreUtils
import com.example.swipeclean.utils.StringUtils.getHumanFriendlyByteCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.stream.Collectors

class RecycleBinActivity : AppCompatActivity() {

    val KEY_INTENT_ALBUM_ID: String = "album_id"
    private val PROGRESS_DIALOG_TAG_DELETE_IMAGE: String = "deleting_images"
    private val PROGRESS_DIALOG_TAG_RESTORE_IMAGE: String = "restoring_images"
    private val MIN_PROGRESS_TIME = 2000L

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: SwipeCleanRecycleBinAdapter
    private lateinit var mEmptyTrashButton: View
    private lateinit var mRestoreAllButton: View
    private lateinit var mBackButton: ImageView
    private lateinit var mTitleTextView: TextView

    private lateinit var mAlbum: Album

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recycle_bin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()

        mAlbum = AlbumController.getInstance(this).albums.stream().filter { item ->
            item.id == intent.getLongExtra(
                KEY_INTENT_ALBUM_ID,
                0
            )
        }
            .findFirst().orElse(null)
        showDeletedPhotos(mAlbum.photos.stream().filter { item -> item.isDelete }
            .collect(Collectors.toList()))
    }

    private fun initView() {
        mRecyclerView = findViewById(R.id.v_recyclerview)
        mEmptyTrashButton = findViewById(R.id.btn_empty_trash)
        mRestoreAllButton = findViewById(R.id.btn_restore_all)
        mBackButton = findViewById(R.id.iv_back)
        mTitleTextView = findViewById(R.id.tv_title)

        mBackButton.setOnClickListener { finish() }
    }

    private fun showDeletedPhotos(deletedPhotos: List<Photo>) {
        if (deletedPhotos.isEmpty()) {
            finish()
            return
        }
        mAdapter = SwipeCleanRecycleBinAdapter(
            deletedPhotos,
            object : SwipeCleanRecycleBinAdapter.OperationListener {
                override fun onItemKeepClick(itemPosition: Int, photo: Photo) {
                    mAdapter.notifyItemRemoved(itemPosition)
                    mAdapter.removePhoto(photo)
                    showTotalSize(mAdapter.totalSize)
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            AlbumController.getInstance(this@RecycleBinActivity)
                                .converseDeleteToKeepPhoto(
                                    photo
                                )
                        }
                    }
                    photo.isKeep = true
                    photo.isDelete = false

                    if (mAdapter.data == null || mAdapter.data.isEmpty()) {
                        finish()
                    }
                }

                override fun onItemClick(itemPosition: Int, photo: Photo?) {

                }
            })

        val spanCount =
            3.coerceAtLeast((Resources.getSystem().displayMetrics.widthPixels / (140 * Resources.getSystem().displayMetrics.density)).toInt())
        val layoutManager = GridLayoutManager(this, spanCount)

        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = layoutManager

        showTotalSize(mAdapter.totalSize)

        mEmptyTrashButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("删除图片")
                .setMessage("一旦删除，图像将无法恢复")
                .setPositiveButton(
                    "删除"
                ) { _, _ ->
                    run {
                        lifecycleScope.launch {
                            val alertDialog: AlertDialog =
                                AlertDialog.Builder(this@RecycleBinActivity).create();
                            alertDialog.setView(
                                LayoutInflater.from(this@RecycleBinActivity)
                                    .inflate(R.layout.view_progress_dialog, null)
                            )
                            alertDialog.setCancelable(false)
                            alertDialog.show()
                            val startTime = SystemClock.elapsedRealtime()

                            SwipeCleanConfigHost.setCleanedSize(
                                mAdapter.getTotalSize(),
                                this@RecycleBinActivity
                            )
                            withContext(Dispatchers.IO) {
                                val destPaths: MutableList<String> = ArrayList()
                                var finishCount = 0

                                for (photo in deletedPhotos) {
                                    finishCount++
                                    destPaths.add(photo.path)
                                    mAlbum.photos.remove(photo)
                                    AlbumController.getInstance(this@RecycleBinActivity)
                                        .cleanCompletedPhoto(photo)

                                    val file = File(photo.path)
                                    if (file.exists()) {
                                        file.delete()
                                    }

                                    if (finishCount % 100 == 0) {
                                        MediaStoreUtils.scanSync(this@RecycleBinActivity, destPaths)
                                        destPaths.clear()
                                    }
                                }

                                MediaStoreUtils.scanSync(this@RecycleBinActivity, destPaths)

                                runOnUiThread {
                                    val spendTime = SystemClock.elapsedRealtime() - startTime
                                    if (spendTime < MIN_PROGRESS_TIME) {
                                        mEmptyTrashButton.postDelayed(
                                            {
                                                alertDialog.dismiss()
                                                showDeleteResult()
                                            },
                                            MIN_PROGRESS_TIME - spendTime
                                        )

                                    } else {
                                        alertDialog.dismiss()
                                        showDeleteResult()
                                    }
                                }
                            }
                        }
                    }
                }
                .setNegativeButton("取消") { _, _ ->

                }
                .show()
        }

        mRestoreAllButton.setOnClickListener {
            showTotalSize(0)
            mRecyclerView.visibility = View.GONE
            lifecycleScope.launch {
                val alertDialog: AlertDialog =
                    AlertDialog.Builder(this@RecycleBinActivity).create();
                alertDialog.setView(
                    LayoutInflater.from(this@RecycleBinActivity)
                        .inflate(R.layout.view_progress_dialog, null)
                )
                alertDialog.setCancelable(false)
                alertDialog.show()
                val startTime = SystemClock.elapsedRealtime()

                withContext(Dispatchers.IO) {
                    for (photo in mAdapter.data) {
                        photo.isDelete = false
                        photo.isKeep = true
                        AlbumController.getInstance(this@RecycleBinActivity)
                            .converseDeleteToKeepPhoto(photo)
                    }

                    runOnUiThread {
                        val spendTime = SystemClock.elapsedRealtime() - startTime
                        if (spendTime < MIN_PROGRESS_TIME) {
                            mEmptyTrashButton.postDelayed(
                                { alertDialog.dismiss() },
                                MIN_PROGRESS_TIME - spendTime
                            )

                        } else {
                            alertDialog.dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteResult() {
        (findViewById<TextView>(R.id.tv_free_up_size)!!).text =
            getHumanFriendlyByteCount(mAdapter.totalSize, 1)
        (findViewById<TextView>(R.id.tv_deleted_count)!!).text =
            String.format(
                Locale.getDefault(),
                "%d张图片",
                mAdapter.data.size
            )

        findViewById<View>(R.id.v_trash_bin).visibility = View.GONE
        findViewById<View>(R.id.v_complete).visibility = View.VISIBLE

        mTitleTextView.text = mAlbum.formatData
        findViewById<View>(R.id.btn_got_it).setOnClickListener { finish() }
    }

    private fun showTotalSize(totalSize: Long) {
        mTitleTextView.text = String.format(
            Locale.getDefault(),
            "%s (%s)",
            "垃圾箱",
            getHumanFriendlyByteCount(totalSize, 1)
        )
    }
}