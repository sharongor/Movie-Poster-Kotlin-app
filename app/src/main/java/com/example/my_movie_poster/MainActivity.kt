package com.example.my_movie_poster

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.text.TextWatcher
import android.text.Editable
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var image1: ImageView
    private lateinit var image2: ImageView
    private lateinit var adapter: ArrayAdapter<String>

    private lateinit var alphaAnim: Animation
    private lateinit var rotateAnim: Animation
    private lateinit var translateAnim: Animation

    private var numberOfTickets = ""
    private var movieLocation  = ""
    private var dateSelection = ""
    private var price = 0

    private val locations = arrayOf(
        R.string.please_select_a_location_txt,
        R.string.tel_aviv_cinema_town_txt,
        R.string.haifa_yes_planet_txt,
        R.string.jerusalem_cinema_city_txt,
        R.string.beer_sheva_planetx_txt,
        R.string.rishon_letsyon_yes_planet_txt
    )

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startAnimationChain()
        titleSetUp()
        openTrailer()
        bottomInfo()
        getTicketsButton()
    }

    private fun getTicketsButton() {

        val getTickets = findViewById<Button>(R.id.ticket_btn)
        getTickets.setOnClickListener {

            stopAnimations()
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.ticket_reservation, null)
            builder.setView(dialogView)
            val dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()

            setupTicketAmount(dialogView)

            createLocationList(dialogView)

            createDateButton(dialogView)

            createSummaryButton(dialog, dialogView)
        }
    }

    private fun setupTicketAmount(dialogView: View) {
        val ticketsAmount = dialogView.findViewById<EditText>(R.id.ticket_amount)
        ticketsAmount.filters = arrayOf<InputFilter>(MinMaxFilter())
        val sharedPrefs0 = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        ticketsAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                numberOfTickets = s.toString()
                val editor0 = sharedPrefs0.edit()
                editor0.putString(numberOfTickets , numberOfTickets)
                editor0.apply()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun createLocationList(dialogView: View) {
        val spinnerBtn = dialogView.findViewById<Spinner>(R.id.mySpinner)
        val locationTxt = dialogView.findViewById<TextView>(R.id.location_txt)
        if (spinnerBtn != null) {

            val locationStrings = locations.map { getString(it) }
                .toTypedArray() // so that the adapter wouldn't find Int instead of Strings
            adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, locationStrings)
            spinnerBtn.adapter = adapter

            spinnerBtn.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>) {}
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    if( position != 0){
                        movieLocation = locationStrings[position]
                        locationTxt.text = movieLocation // show the current selection on the button
                    }
                }
            }
        }
    }

    private fun createDateButton(dialogView: View) {
        val dateBtn = dialogView.findViewById<Button>(R.id.btn_date)
        val sharedPrefs1 = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        dateBtn.setOnClickListener {

            val c = Calendar.getInstance()

            val listener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                dateSelection = "$day-${month + 1}-$year"
                dateBtn.text = dateSelection
                val editor1 = sharedPrefs1.edit()
                editor1.putString(dateSelection , dateSelection)
                editor1.apply()
            }
            val dtd = DatePickerDialog(
                this,
                listener,
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            )
            dtd.datePicker.minDate = c.timeInMillis // set min date to be today
            dtd.show()
        }
    }

    private fun createSummaryButton(dialog: AlertDialog, dialogView: View) {
        val summaryBtn = dialogView.findViewById<Button>(R.id.summary_btn)
        summaryBtn.setOnClickListener {

            if (dateSelection.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_choose_a_date_toast), Toast.LENGTH_SHORT
                ).show()
            } else if (movieLocation == "----------" ||  movieLocation.isEmpty()) { // the "---------" is equal across all languages
                Toast.makeText(
                    this,
                    getString(R.string.please_choose_a_location_toast), Toast.LENGTH_SHORT
                ).show()
            } else if (numberOfTickets.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_the_number_of_tickets_toast),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                dialog.dismiss()
                showConfirmationDialog()
            }
        }
    }

    private fun showConfirmationDialog() {

        val confirmationBuilder = AlertDialog.Builder(this)
        val confirmationDialogView = layoutInflater.inflate(R.layout.ticket_summary, null)
        confirmationBuilder.setView(confirmationDialogView)
        val confirmationDialog = confirmationBuilder.create()
        confirmationDialog.setCanceledOnTouchOutside(false)
        confirmationDialog.show()

        val finalLocation = confirmationDialogView.findViewById<TextView>(R.id.location_final)
        val finalDate = confirmationDialogView.findViewById<TextView>(R.id.date_final)
        val totalTickets = confirmationDialogView.findViewById<TextView>(R.id.tickets_Total)
        val totalPrice = confirmationDialogView.findViewById<TextView>(R.id.price_Total)
        val backHome = confirmationDialogView.findViewById<Button>(R.id.home_page)

        price = Integer.valueOf(numberOfTickets) * 40 // randomly picked 40 to be the price of a ticket

        totalTickets.text = getString(R.string.total_tickets_txt_txt, numberOfTickets)
        finalDate.text = getString(R.string.selected_date_txt_txt, dateSelection)
        finalLocation.text = getString(R.string.selected_location_txt_txt, movieLocation)
        totalPrice.text = getString(R.string.selected_price_txt_txt, price.toString())

        backHome.setOnClickListener {
            confirmationDialog.dismiss()
        }
    }

    private fun titleSetUp() {
        val title = findViewById<TextView>(R.id.title)
        title.setOnClickListener {
            val rotate = ObjectAnimator.ofFloat(title, "rotationY", 0f, 360f)
            rotate.duration = 1000
            rotate.start()
        }
    }

    @SuppressLint("InflateParams")
    private fun bottomInfo() {
        val extraInfo = findViewById<Button>(R.id.knowledge_btn)
        extraInfo.setOnClickListener {
            val bottomDialog = BottomSheetDialog(this)
            val bottomView = layoutInflater.inflate(R.layout.bottom_info_dialog, null)
            bottomDialog.setContentView(bottomView)
            bottomDialog.show()
        }
    }

    private fun openTrailer() {
        val openTrailer = findViewById<Button>(R.id.trailer_btn)
        openTrailer.setOnClickListener {

            stopAnimations()

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://youtu.be/2LqzF5WauAw?si=oySoZCBOMhn3-aV0")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage("com.google.android.youtube")
            startActivity(intent)
        }
    }

    // Custom class to define min and max for the tickets selection
    inner class MinMaxFilter : InputFilter {

        private var intMin: Int = 1
        private var intMax: Int = 20

        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned,
            dStart: Int,
            dEnd: Int
        ): CharSequence? {
            try {
                val input = Integer.parseInt(dest.toString() + source.toString())
                if (isInRange(intMin, intMax, input)) {
                    return null
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
            return ""
        }
        // Check if input res is in between min and max
        private fun isInRange(min: Int, max: Int, res: Int): Boolean {
            return res in min..max
        }
    }

    private fun startAnimationChain() {
        alphaAnim = AnimationUtils.loadAnimation(this, R.anim.fade)
        rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate)
        translateAnim = AnimationUtils.loadAnimation(this, R.anim.translate)

        image1 = findViewById(R.id.image_1)
        image2 = findViewById(R.id.image_2)

        image1.startAnimation(alphaAnim)

        translateAnim.setAnimationListener(object : SimpleAnimationListener() {
            override fun onAnimationEnd(animation: Animation?) {
                image2.startAnimation(rotateAnim)
            }
        })
        image2.startAnimation(translateAnim)
    }

    private fun stopAnimations() { // useful feature for stopping animations when moving to dialogs
        image1.clearAnimation()
        image2.clearAnimation()
    }
}

open class SimpleAnimationListener : Animation.AnimationListener {
    override fun onAnimationStart(animation: Animation?) {}
    override fun onAnimationEnd(animation: Animation?) {}
    override fun onAnimationRepeat(animation: Animation?) {}
}