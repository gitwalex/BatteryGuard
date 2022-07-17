package com.gerwalex.batteryguard.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.DB
import com.gerwalex.batteryguard.database.DB.dao
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.databinding.MainFragmentBinding
import com.gerwalex.lib.database.LiveCursorNew
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.text.SimpleDateFormat

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {

    private val chargeData = LiveCursorNew(DB.get(), "Event") {
        dao.getEventList()
    }
    private val timeFormatter = object : ValueFormatter() {
        val timeFormat = SimpleDateFormat("HH:mm:ss")
        override fun getFormattedValue(value: Float): String {
            return timeFormat.format(value)
        }
    }
    private val firstdataSet = ArrayList<ILineDataSet>()
    private val secondDataSet = ArrayList<ILineDataSet>()
    private lateinit var binding: MainFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = MainFragmentBinding.inflate(inflater, container, false)
        binding.chargeStateChart.xAxis.valueFormatter = timeFormatter
        binding.voltChart.xAxis.valueFormatter = timeFormatter

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_MainFragment_to_EventListFragment)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chargeData.observe(viewLifecycleOwner) { c ->
            if (c.moveToFirst()) {
                var event: Event
                val chargeLevelEntries = ArrayList<Entry>()
                val temperaturEntries = ArrayList<Entry>()
                val voltEntries = ArrayList<Entry>()
                do {
                    event = Event(c)
                    chargeLevelEntries.add(Entry(event.ts.toFloat(), event.remaining.toFloat()))
                    temperaturEntries.add(Entry(event.ts.toFloat(), event.temperature / 10f))
                    voltEntries.add(Entry(event.ts.toFloat(), event.voltage.toFloat()))
                } while (c.moveToNext())
                binding.event = event
                firstdataSet.clear()
                firstdataSet.add(getChargingLevelLine(chargeLevelEntries))
                firstdataSet.add(getTemperaturLine(temperaturEntries))
                binding.chargeStateChart.data = LineData(firstdataSet)
                binding.chargeStateChart.notifyDataSetChanged()
                binding.chargeStateChart.invalidate()
//
                secondDataSet.clear()
                secondDataSet.add(getVoltLine(voltEntries))
                binding.voltChart.data = LineData(secondDataSet)
                binding.voltChart.notifyDataSetChanged()
                binding.voltChart.invalidate()
            }
        }
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
        return LineDataSet(entries, getString(R.string.temperatur)).also {
            it.setDrawCircles(false)
            it.setDrawValues(false)
            it.axisDependency = YAxis.AxisDependency.RIGHT
            it.lineWidth = 1.5f
            it.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            it.color = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
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
}