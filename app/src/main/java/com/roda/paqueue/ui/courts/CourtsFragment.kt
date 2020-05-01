package com.roda.paqueue.ui.courts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.roda.paqueue.R

class CourtsFragment : Fragment() {

    private lateinit var courtsViewModel: CourtsViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        courtsViewModel =
                ViewModelProviders.of(this).get(CourtsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_courts, container, false)
        val textView: TextView = root.findViewById(R.id.text_notifications)
        courtsViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }
}
