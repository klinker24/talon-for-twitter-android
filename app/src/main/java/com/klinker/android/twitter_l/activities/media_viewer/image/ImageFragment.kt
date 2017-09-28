package com.klinker.android.twitter_l.activities.media_viewer.image

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.klinker.android.twitter_l.R
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.support.v4.content.FileProvider
import android.os.Environment.getExternalStorageDirectory
import android.support.v4.app.NotificationCompat
import com.klinker.android.twitter_l.BuildConfig
import com.klinker.android.twitter_l.settings.AppSettings
import com.klinker.android.twitter_l.utils.*
import com.klinker.android.twitter_l.utils.api_helper.TwitterDMPicHelper
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class ImageFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.fragment_image, container, false)

        val imageLink = getLink(arguments)
        val imageView = root.findViewById<View>(R.id.imageView) as ImageView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && arguments.getInt(EXTRA_INDEX, 0) != 0) {
            imageView.transitionName = ""
        }

        Glide.with(this).load(imageLink).diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<String, GlideDrawable> {
                    override fun onException(e: Exception, model: String, target: Target<GlideDrawable>, isFirstResource: Boolean): Boolean = false
                    override fun onResourceReady(resource: GlideDrawable, model: String, target: Target<GlideDrawable>, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                        activity.supportStartPostponedEnterTransition()
                        return false
                    }
                }).into(imageView)

        Handler().postDelayed({ activity.supportStartPostponedEnterTransition() }, 500)
        DraggablePhotoViewAttacher(activity as AppCompatActivity, imageView)

        imageView.post({
            imageView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            imageView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            imageView.invalidate()
        })

        return root
    }

    companion object {
        private val EXTRA_URL = "extra_url"
        private val EXTRA_INDEX = "extra_index"

        fun getInstance(index: Int, imageLink: String): ImageFragment {
            val b = Bundle()
            b.putString(EXTRA_URL, imageLink)
            b.putInt(EXTRA_INDEX, index)

            val fragment = ImageFragment()
            fragment.arguments = b

            return fragment
        }

        private fun getLink(args: Bundle): String {
            val url = args.getString(EXTRA_URL)
            if (url.contains("imgur")) return url.replace("t.jpg", ".jpg")
            if (url.contains("insta")) return url.substring(0, url.length - 1) + "l"

            return url
        }
    }





    //
    //
    // Long methods for downloading and sharing images in the fragment
    //
    //


    fun downloadImage() {
        TimeoutThread({
            Looper.prepare()
            val url = getLink(arguments)

            try {
                val mBuilder = NotificationCompat.Builder(context, NotificationChannelUtil.MEDIA_DOWNLOAD_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setTicker(resources.getString(R.string.downloading) + "...")
                        .setContentTitle(resources.getString(R.string.app_name))
                        .setContentText(resources.getString(R.string.saving_picture) + "...")
                        .setProgress(100, 100, true)

                val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(6, mBuilder.build())

                val bitmap = if (url.contains("ton.twitter.com") || url.contains("twitter.com/messages/")) {
                    // it is a direct message picture
                    val helper = TwitterDMPicHelper()
                    helper.getDMPicture(url, Utils.getTwitter(context, AppSettings.getInstance(context)), context)
                } else {
                    var urlString = url
                    if (urlString.contains("pbs.twimg")) {
                        urlString += ":orig"
                    }

                    val conn = URL(urlString).openConnection() as HttpURLConnection
                    val inputStream = BufferedInputStream(conn.getInputStream())

                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = false

                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    bitmap
                }

                val name = "Image-" + Random().nextInt(1000000)


                var uri = IOUtils.saveImage(bitmap, name, context)
                val root = Environment.getExternalStorageDirectory().toString()
                val myDir = File(root + "/Talon")
                val file = File(myDir, name + ".jpg")

                try {
                    uri = FileProvider.getUriForFile(context,
                            BuildConfig.APPLICATION_ID + ".provider", file)
                } catch (err: Exception) {
                }

                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.setDataAndType(uri, "image/*")

                val randomId = NotificationUtils.generateRandomId()
                val pending = PendingIntent.getActivity(context, randomId, intent, 0)

                val builder2 = NotificationCompat.Builder(context, NotificationChannelUtil.MEDIA_DOWNLOAD_CHANNEL)
//                                    .setContentIntent(pending)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setTicker(resources.getString(R.string.saved_picture) + "...")
                        .setContentTitle(resources.getString(R.string.app_name))
                        .setAutoCancel(true)
                        .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                        .setContentText(resources.getString(R.string.saved_picture) + "!")

                mNotificationManager.cancel(6)
                mNotificationManager.notify(randomId, builder2.build())
            } catch (e: Exception) {
                e.printStackTrace()
                activity.runOnUiThread({
                    try {
                        PermissionModelUtils(context).showStorageIssue(e)
                    } catch (x: Exception) {
                        e.printStackTrace()
                    }
                })

                val builder2 = NotificationCompat.Builder(context, NotificationChannelUtil.MEDIA_DOWNLOAD_CHANNEL)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setTicker(resources.getString(R.string.error) + "...")
                        .setContentTitle(resources.getString(R.string.app_name))
                        .setContentText(resources.getString(R.string.error) + "...")
                        .setProgress(0, 100, true)

                val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.notify(6, builder2.build())
            }
        }).start()
    }

    fun shareImage() {
        TimeoutThread(Runnable {
            try {
                val bitmap = Glide.with(this@ImageFragment)
                        .load(getLink(arguments))
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                        .get()

                activity.runOnUiThread({
                    // create the intent
                    val sharingIntent = Intent(android.content.Intent.ACTION_SEND)
                    sharingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    sharingIntent.type = "image/*"

                    // add the bitmap uri to the intent
                    val uri = getImageUri(activity, bitmap)
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)

                    // start the chooser
                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.menu_share) + ": "))
                })
            } catch (e: Exception) {

            }
        }).start()
    }

    private fun getImageUri(context: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val f = File(getExternalStorageDirectory().path + "/Talon/image_to_share.jpg")
        val dir = File(Environment.getExternalStorageDirectory(), "Talon")

        return try {
            if (!dir.exists()) dir.mkdirs()
            f.createNewFile()

            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())

            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", f)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


}