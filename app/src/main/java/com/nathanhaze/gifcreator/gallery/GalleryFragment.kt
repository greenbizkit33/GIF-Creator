package com.nathanhaze.gifcreator.gallery

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.nathanhaze.gifcreator.GifCreatorApp
import com.nathanhaze.gifcreator.R
import com.nathanhaze.gifcreator.event.ImageSelectedEvent
import com.nathanhaze.gifcreator.event.RefeshGalleryEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class GalleryFragment : Fragment() {

    private var recyclerview: RecyclerView? = null
    private var adapter: PhotoGalleryAdapter? = null
    private var tvNoImages: TextView? = null
    private var galleryControls: LinearLayout? = null
    private var trashButton: ImageView? = null
    private var application: GifCreatorApp? = null
    private var list: List<String?>? = null
    var defaultImage: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)
        val mAdView = view.findViewById<AdView>(R.id.adView)
        if (mAdView != null) {
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
        } else {
            mAdView?.visibility = View.GONE
        }
        application = requireActivity().application as GifCreatorApp
        val layout = GridLayoutManager(application!!.applicationContext, 2)
        recyclerview = view.findViewById(R.id.rv_gallery)
        galleryControls = view.findViewById(R.id.ll_gallery_controls)
        galleryControls?.translationX = -CONTROLS_DISTANCE.toFloat()
        trashButton = view.findViewById(R.id.iv_delete)
        val tvSelectAll = view.findViewById<ImageView>(R.id.tv_select_all)
        list = application?.getListFiles()
        tvNoImages = view.findViewById(R.id.tv_no_images)
        if (list != null && list!!.size != 0) {
            adapter = PhotoGalleryAdapter(list)
            recyclerview?.adapter = adapter
            recyclerview?.layoutManager = layout
            if (defaultImage != null && !defaultImage!!.isEmpty()) {
                var index = 0
                var i = 0
                for (s in list!!) {
                    if (defaultImage == s) {
                        index = i
                        break
                    }
                    i++
                }
                val indexImage = i
                if (i > 1) {
                    recyclerview?.viewTreeObserver
                        ?.addOnGlobalLayoutListener(
                            object : ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    if (recyclerview?.measuredHeight!! > 0) {
                                        Handler().postDelayed(Runnable { //  layout.scrollToPosition(20);
                                            //  recyclerview.smoothScrollToPosition(10);
                                            //  layout.scrollToPositionWithOffset(20, 0);
                                            if (activity == null) {
                                                return@Runnable
                                            }
                                            val smoothScroller: SmoothScroller =
                                                object : LinearSmoothScroller(activity) {
                                                    override fun getVerticalSnapPreference(): Int {
                                                        return SNAP_TO_START
                                                    }
                                                }
                                            smoothScroller.targetPosition = indexImage
                                            layout.startSmoothScroll(smoothScroller)
                                        }, 100)
                                        recyclerview!!
                                            .viewTreeObserver
                                            .removeOnGlobalLayoutListener(this)
                                    }
                                }
                            })
                }
            }
            tvNoImages?.visibility = View.GONE
        } else {
            tvNoImages?.visibility = View.VISIBLE
        }
        tvSelectAll.setOnClickListener {
            val temp = ArrayList<String?>()
            for (fileName in list as MutableList<String?>) {
                temp.add(fileName)
            }
            adapter!!.setImageCheckedMap(temp)
            adapter!!.notifyDataSetChanged()
        }
        trashButton?.setOnClickListener { _: View? ->
            showPopup()
        }
        EventBus.getDefault().register(this)
        return view
    }

    fun showPopup() {
        val alertDialog = AlertDialog.Builder(activity).create()
        alertDialog.setCancelable(true)
        alertDialog.setMessage(
            resources.getString(R.string.delete) + " " + adapter!!.selectedCount + " " + resources.getString(
                R.string.images
            ).lowercase(
                Locale.getDefault()
            )
        )
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            resources.getString(R.string.ok)
        ) { _, _ ->
            val images = adapter!!.imagesSelected
            for (imagePath in images) {
                var file: File? = null
                try {
                    file = File(imagePath)
                } catch (ex: IndexOutOfBoundsException) {
                }
                val deleted = file!!.delete()
                if (deleted) {
                    trashButton?.context?.sendBroadcast(
                        Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.fromFile(file)
                        )
                    )
                }
            }
            list = application?.getListFiles()
            if ((list as MutableList<String?>).size > 0) {
                adapter = PhotoGalleryAdapter(list)
                recyclerview?.adapter = adapter
            } else {
                recyclerview?.visibility = View.GONE
                tvNoImages?.visibility = View.VISIBLE
                galleryControls?.visibility = View.GONE
                galleryControls?.animate()?.translationY(-CONTROLS_DISTANCE.toFloat())
            }
        }
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE,
            resources.getString(R.string.cancel)
        ) { _, _ -> alertDialog.dismiss() }
        alertDialog.show()
    }

    override fun onResume() {
        super.onResume()
        list = application?.getListFiles()
        if (list != null && list!!.size == 0) {
            tvNoImages!!.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        EventBus.getDefault().unregister(this)
        super.onDestroyView()
    }

    @Subscribe
    fun onEvent(event: ImageSelectedEvent) {
        if (event.hasSelections) {
            // galleryControls.animate().setDuration(1000).
            galleryControls!!.visibility = View.VISIBLE
        } else {
            //   galleryControls.animate().translationY(-CONTROLS_DISTANCE);
            galleryControls!!.visibility = View.GONE
        }
    }

    @Subscribe
    fun onEvent(event: RefeshGalleryEvent?) {
        val list = application?.getListFiles()
        (recyclerview!!.adapter as PhotoGalleryAdapter?)!!.setDataSet(list)
        recyclerview!!.adapter!!.notifyDataSetChanged()
        if (list?.size == 0) {
            tvNoImages!!.visibility = View.VISIBLE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpFromSameTask(requireActivity())
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openImage(photo: File) {
        if (activity == null) {
            return
        }
        val intent = Intent(activity, PhotoPagerActivity::class.java)
        intent.putExtra("path", photo.absolutePath)
        startActivity(intent)
    }

    companion object {
        private const val CONTROLS_DISTANCE = 40
    }
}