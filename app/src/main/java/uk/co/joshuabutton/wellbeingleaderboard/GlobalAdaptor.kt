package uk.co.joshuabutton.wellbeingleaderboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.leaderboard_layout.view.*
import uk.co.joshuabutton.wellbeingleader.userItem

class GlobalAdaptor(private val globalLBList: List<userItem?>) : RecyclerView.Adapter<GlobalAdaptor.globalViewHolder>() {

    public lateinit var context : Context

    var GlobalFragment = GlobalFragment()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): globalViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.leaderboard_layout,
            parent, false
        )

        var context = parent.context

        return globalViewHolder(itemView)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: globalViewHolder, position: Int) {
        val currentItem = globalLBList[position]

        if(currentItem?.uid == MainActivity.currentUser?.uid){
            GlobalFragment.myPostion = position
            holder.cardWidget.setCardBackgroundColor(R.color.colorAccent)
            holder.nameText.setTextColor(Color.parseColor("#FFFFFF"))
            holder.countryText.setTextColor(Color.parseColor("#FFFFFF"))
            holder.appTimeText.setTextColor(Color.parseColor("#FFFFFF"))
            holder.leaderBoardPosition.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            holder.cardWidget.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
            holder.nameText.setTextColor(Color.parseColor("#000000"))
            holder.countryText.setTextColor(Color.parseColor("#000000"))
            holder.appTimeText.setTextColor(Color.parseColor("#000000"))
            holder.leaderBoardPosition.setTextColor(Color.parseColor("#000000"))
        }

        if (currentItem != null) {
            holder.nameText.text = currentItem.name
        }
        if (currentItem != null) {
            holder.countryText.text = currentItem.country
        }
        if (currentItem != null) {
            holder.appTimeText.text = currentItem.displayAppTime
        }
            holder.leaderBoardPosition.text = ordinalOf(position+1)
    }

    fun ordinalOf(i: Int) = "$i" + if (i % 100 in 11..13) "th" else when (i % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }

    override fun getItemCount() = globalLBList.size

    class globalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.leaderBoardName
        val countryText: TextView = itemView.leaderBoardCountry
        val appTimeText: TextView = itemView.leaderBoardTime
        val leaderBoardPosition: TextView = itemView.leaderBoardPosition
        val cardWidget: CardView = itemView.cardView
    }
}