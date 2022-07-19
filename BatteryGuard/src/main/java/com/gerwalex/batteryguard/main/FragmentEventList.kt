package com.gerwalex.batteryguard.main

import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gerwalex.batteryguard.R
import com.gerwalex.batteryguard.database.DB
import com.gerwalex.batteryguard.database.DB.dao
import com.gerwalex.batteryguard.database.tables.Event
import com.gerwalex.batteryguard.databinding.BatteryEventListItemBinding
import com.gerwalex.batteryguard.databinding.EventListBinding
import com.gerwalex.lib.adapters.CursorAdapter
import com.gerwalex.lib.adapters.ViewHolder
import com.gerwalex.lib.database.LiveCursorNew

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class FragmentEventList : Fragment() {

    private val data = LiveCursorNew(DB.get(), "Event") {
        dao.getEventListDesc()
    }
    private lateinit var binding: EventListBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = EventListBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = EventListAdapter()
        binding.recyclerView.adapter = adapter
        data.observe(viewLifecycleOwner) {
            adapter.swap(it)
        }
    }

    inner class EventListAdapter : CursorAdapter() {

        override fun getItemViewType(position: Int): Int {
            return R.layout.battery_event_list_item
        }

        override fun onBindViewHolder(holder: ViewHolder, mCursor: Cursor, position: Int) {
            val binding = holder.binding as BatteryEventListItemBinding
            val event = Event(mCursor)
            binding.event = event
            Log.d("gerwalex", "Pos: $position, event: $event ")
        }
    }
}