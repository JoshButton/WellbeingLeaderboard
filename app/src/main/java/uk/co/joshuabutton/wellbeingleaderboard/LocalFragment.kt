package uk.co.joshuabutton.wellbeingleaderboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_local.view.*

class LocalFragment : Fragment() {

    lateinit var recyclerView: RecyclerView
    private lateinit var viewOfLayout: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewOfLayout = inflater.inflate(R.layout.fragment_local, container, false)
        recyclerView = viewOfLayout.globalRecyclerView

        initRecylcer()

        return viewOfLayout
    }

    public fun initRecylcer() {
        recyclerView.adapter = GlobalAdaptor((activity as? MainActivity)!!.localLBList.sortedWith(compareUsers))
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
    }
}
