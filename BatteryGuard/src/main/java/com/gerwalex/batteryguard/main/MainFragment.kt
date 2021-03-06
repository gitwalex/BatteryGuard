package com.gerwalex.batteryguard.main

import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.DB.dao
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.databinding.MainFragmentBinding
import com.gerwalex.batteryguard.system.BatteryGuardService
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {

    private val serviceConnection = BatteryServiceConnection()
    private val timeFormatter = object : ValueFormatter() {
        val timeFormat = SimpleDateFormat("HH:mm:ss")
        override fun getFormattedValue(value: Float): String {
            return timeFormat.format(value)
        }
    }
    private val chargeLevelEntries = ArrayList<Entry>()
    private val temperaturEntries = ArrayList<Entry>()
    private val voltEntries = ArrayList<Entry>()
    private lateinit var binding: MainFragmentBinding
    private val menuProvider: MenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_MainFragment_to_batteryGuardPreferenceActivity)
                    return true
                }
                R.id.action_powerusage -> {
                    startActivityWithCheck(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
                    return true
                }
                R.id.action_powersettings -> {
                    startActivityWithCheck(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                    return true
                }
            }
            return false
        }
    }

    private fun startActivityWithCheck(intent: Intent) {
        intent
            .resolveActivity(requireActivity().packageManager)
            ?.let {
                startActivity(intent)
            } ?: run {
            Snackbar
                .make(requireView(), R.string.sorry_no_activity_found, Snackbar.LENGTH_LONG)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireContext().unbindService(serviceConnection)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = MainFragmentBinding
            .inflate(inflater, container, false)
            .apply {
                chargeStateChart.xAxis.valueFormatter = timeFormatter
                voltChart.xAxis.valueFormatter = timeFormatter
                fab.setOnClickListener {
                    findNavController().navigate(R.id.action_MainFragment_to_EventListFragment)
                }
            }
        requireContext().run {
            bindService(Intent(this, BatteryGuardService::class.java), serviceConnection, BIND_AUTO_CREATE)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.addMenuProvider(menuProvider)
    }

    private fun getVoltLine(entries: List<Entry>): LineDataSet {
        return LineDataSet(entries, getString(R.string.voltage)).also {
            it.setDrawCircles(false)
            it.setDrawValues(false)
            it.axisDependency = YAxis.AxisDependency.LEFT
            it.lineWidth = 1.5f
            it.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            it.color = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
        }
    }

    private fun getTemperaturLine(entries: List<Entry>): LineDataSet {
        return LineDataSet(entries, getString(R.string.temperatur)).apply {
            setDrawCircles(false)
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.LEFT
            lineWidth = 1.5f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            color = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
        }
    }

    private fun getChargingLevelLine(entries: List<Entry>): LineDataSet {
        return LineDataSet(entries, getString(R.string.chargeState)).apply {
            setDrawCircles(false)
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.LEFT
            lineWidth = 1.5f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            color = ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light)
        }
    }

    inner class BatteryServiceConnection : ServiceConnection {

        private lateinit var myService: BatteryGuardService
        private val observer = Observer<Event> { event ->
            Log.d("gerwalex", "Event: $event ")
            binding.event = event
            with(binding) {
                val firstdataSet = ArrayList<ILineDataSet>()
                val secondDataSet = ArrayList<ILineDataSet>()
                chargeLevelEntries.add(Entry(event.ts.toFloat(), event.remaining.toFloat()))
                temperaturEntries.add(Entry(event.ts.toFloat(), event.temperature / 10f))
                voltEntries.add(Entry(event.ts.toFloat(), event.voltage.toFloat()))
                firstdataSet.add(getChargingLevelLine(chargeLevelEntries))
                firstdataSet.add(getTemperaturLine(temperaturEntries))
                secondDataSet.add(getVoltLine(voltEntries))
                val firstData = LineData(firstdataSet)
                val secondData = LineData(secondDataSet)
                chargeStateChart.data = firstData
                chargeStateChart.data.notifyDataChanged()
                chargeStateChart.notifyDataSetChanged()
                chargeStateChart.moveViewToX(event.ts.toFloat())
                chargeStateChart.invalidate()
                voltChart.data = secondData
                voltChart.data.notifyDataChanged()
                voltChart.notifyDataSetChanged()
                voltChart.moveViewToX(event.ts.toFloat())
                voltChart.invalidate()
            }
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("gerwalex", "Service connected: $name")
            lifecycleScope.launch {
                chargeLevelEntries.clear()
                temperaturEntries.clear()
                voltEntries.clear()
                withContext(Dispatchers.IO) {
                    val from = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS
                    val c = dao.getEventList(from)
                    if (c.moveToFirst()) {
                        var event: Event
                        do {
                            event = Event(c)
                            chargeLevelEntries.add(Entry(event.ts.toFloat(), event.remaining.toFloat()))
                            temperaturEntries.add(Entry(event.ts.toFloat(), event.temperature / 10f))
                            voltEntries.add(Entry(event.ts.toFloat(), event.voltage.toFloat()))
                        } while (c.moveToNext())
                    }
                }
                service?.let {
                    myService = (it as BatteryGuardService.BatteryServiceBinder).service
                    myService.lastEvent.observe(viewLifecycleOwner, observer)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("gerwalex", "Service disconnected: $name")
        }
    }
}