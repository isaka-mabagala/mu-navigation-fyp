package com.isaka.munavigation.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.isaka.munavigation.R
import com.isaka.munavigation.model.Category
import com.isaka.munavigation.model.Faq
import com.isaka.munavigation.model.Location
import com.isaka.munavigation.util.Constants
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

open class BaseActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false
    private lateinit var mProgressDialog: Dialog

    // double click back to close app
    fun doubleBackToExit() {
        if (doubleBackToExitPressedOnce) {
            finishAffinity()
            exitProcess(0)
        }
        this.doubleBackToExitPressedOnce = true

        showExitSnackBar(resources.getString(R.string.please_click_back_again_to_exit))

        Handler(Looper.getMainLooper()).postDelayed({
            this.doubleBackToExitPressedOnce = false
        }, 2000)
    }

    // progress dialog show
    fun showProgressDialog(text: String) {
        mProgressDialog = Dialog(this)
        mProgressDialog.setContentView(R.layout.dialog_progress)
        mProgressDialog.setCancelable(false)

        val body = mProgressDialog.findViewById(R.id.tv_progress_text) as TextView
        body.text = text

        mProgressDialog.show()
    }

    // progress dialog hide
    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }

    // error snack bar with red background color
    fun showErrorSnackBar(message: String) {
        val snackBar = Snackbar.make(
            findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG
        )
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(
            ContextCompat.getColor(
                this, R.color.scarlet
            )
        )
        snackBar.show()
    }

    // success snack bar
    fun showSuccessSnackBar(message: String) {
        val snackBar = Snackbar.make(
            findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG
        )
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(
            ContextCompat.getColor(
                this, R.color.gunmetal_800
            )
        )
        snackBar.show()
    }

    // info snack bar
    fun showInfoSnackBar(message: String) {
        val snackBar = Snackbar.make(
            findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG
        )
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(
            ContextCompat.getColor(
                this, R.color.teal_700
            )
        )
        snackBar.show()
    }

    // exit snack bar
    private fun showExitSnackBar(message: String) {
        val snackBar = Snackbar.make(
            findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT
        )
        val snackBarView = snackBar.view
        val snackBarText = snackBarView.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        )
        snackBarView.setBackgroundColor(
            ContextCompat.getColor(
                this, R.color.gunmetal
            )
        )
        snackBarText.setTextColor(
            ContextCompat.getColor(
                this, R.color.sandy_brown_800
            )
        )
        snackBar.show()
    }

    // ISO 8601 to date
    fun getDateFromISO(isoStr: String): Date? {
        // isoStr = 2022-04-12T12:06:52.848Z
        var mDate: Date? = null

        try {
            val formatter = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()
            )
            formatter.timeZone = TimeZone.getDefault()
            mDate = formatter.parse(isoStr)
            // Log.e("mDate", mDate.toString())
        } catch (e: Exception) {
            Log.e("mDate", e.toString())
        }

        return mDate
    }

    // number format
    fun numberFormat(number: Double): String {
        return "%,.2f".format(Locale.ENGLISH, number)
    }

    // get list of frequency asked questions
    fun getFaqList(context: Context = applicationContext): List<Faq> {
        val jsonFileString = getJsonDataFromAsset(context, Constants.FAQ_FILE)
        val listFaqType = object : TypeToken<List<Faq>>() {}.type

        return Gson().fromJson(jsonFileString, listFaqType)
    }

    // get list of location category
    fun getCategoryList(context: Context = applicationContext): List<Category> {
        val jsonFileString = getJsonDataFromAsset(context, Constants.CATEGORY_FILE)
        val listCategoryType = object : TypeToken<List<Category>>() {}.type

        return Gson().fromJson(jsonFileString, listCategoryType)
    }

    // get location category by alias
    fun getCategoryByAlias(alias: String, context: Context = applicationContext): Category {
        val jsonFileString = getJsonDataFromAsset(context, Constants.CATEGORY_FILE)
        val listCategoryType = object : TypeToken<List<Category>>() {}.type
        val categoryList: List<Category> = Gson().fromJson(jsonFileString, listCategoryType)

        return categoryList.filter { it.alias == alias }[0]
    }

    // get list of location
    fun getLocationList(context: Context = applicationContext): List<Location> {
        val jsonFileString = getJsonDataFromAsset(context, Constants.LOCATION_FILE)
        val listLocationType = object : TypeToken<List<Location>>() {}.type

        return Gson().fromJson(jsonFileString, listLocationType)
    }

    // get list of location by category
    fun getLocationListByCategory(
        category: String, context: Context = applicationContext
    ): List<Location> {
        val jsonFileString = getJsonDataFromAsset(context, Constants.LOCATION_FILE)
        val listLocationType = object : TypeToken<List<Location>>() {}.type
        val locationList: List<Location> = Gson().fromJson(jsonFileString, listLocationType)

        return locationList.filter { it.category.lowercase().contains(category.lowercase()) }
    }

    // get location by ID
    fun getLocationById(id: String, context: Context = applicationContext): Location {
        val jsonFileString = getJsonDataFromAsset(context, Constants.LOCATION_FILE)
        val listLocationType = object : TypeToken<List<Location>>() {}.type
        val locationList: List<Location> = Gson().fromJson(jsonFileString, listLocationType)

        return locationList.filter { it.id == id }[0]
    }

    // get drawable resource id by name
    @SuppressLint("DiscouragedApi")
    fun drawableResourceId(name: String, context: Context = applicationContext): Int {
        val uri = "@drawable/$name"
        return context.resources.getIdentifier(uri, null, context.packageName)
    }

    // show rational dialog for permissions
    fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this).setMessage(R.string.rational_dialog_for_permissions)
            .setPositiveButton(R.string.positive_button_permission) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)

                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton(R.string.negative_button_permission) { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    // get data from json file
    private fun getJsonDataFromAsset(context: Context, fileName: String): String {
        val jsonString: String
        try {
            jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return ""
        }
        return jsonString
    }
}