package ca.etrak.wifiautoconnect.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.etrak.wifiautoconnect.R
import ca.etrak.wifiautoconnect.data.ConnectionLog
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter : ListAdapter<ConnectionLog, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconEvent: ImageView = itemView.findViewById(R.id.iconEvent)
        private val textSsid: TextView = itemView.findViewById(R.id.textSsid)
        private val textEventType: TextView = itemView.findViewById(R.id.textEventType)
        private val textDetails: TextView = itemView.findViewById(R.id.textDetails)
        private val textTimestamp: TextView = itemView.findViewById(R.id.textTimestamp)

        private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        fun bind(log: ConnectionLog) {
            textSsid.text = log.ssid
            textTimestamp.text = dateFormat.format(Date(log.timestamp))
            textDetails.text = log.details ?: "Signal: ${log.signalStrength} dBm"

            val (eventText, eventColor, eventIcon) = when (log.eventType) {
                ConnectionLog.EVENT_SCAN -> Triple(
                    "Détecté",
                    R.color.event_scan,
                    R.drawable.ic_scan
                )
                ConnectionLog.EVENT_CONNECT_ATTEMPT -> Triple(
                    "Tentative",
                    R.color.event_attempt,
                    R.drawable.ic_connecting
                )
                ConnectionLog.EVENT_CONNECT_SUCCESS -> Triple(
                    "Connecté",
                    R.color.event_success,
                    R.drawable.ic_wifi_connected
                )
                ConnectionLog.EVENT_CONNECT_FAILED -> Triple(
                    "Échec",
                    R.color.event_failed,
                    R.drawable.ic_wifi_off
                )
                ConnectionLog.EVENT_DISCONNECT -> Triple(
                    "Déconnecté",
                    R.color.event_disconnect,
                    R.drawable.ic_disconnect
                )
                else -> Triple(
                    log.eventType,
                    R.color.event_scan,
                    R.drawable.ic_info
                )
            }

            textEventType.text = eventText
            textEventType.setTextColor(ContextCompat.getColor(itemView.context, eventColor))
            iconEvent.setImageResource(eventIcon)
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<ConnectionLog>() {
        override fun areItemsTheSame(oldItem: ConnectionLog, newItem: ConnectionLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ConnectionLog, newItem: ConnectionLog): Boolean {
            return oldItem == newItem
        }
    }
}
