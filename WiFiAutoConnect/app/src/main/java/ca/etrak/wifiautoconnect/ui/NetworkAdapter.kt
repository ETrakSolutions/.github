package ca.etrak.wifiautoconnect.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ca.etrak.wifiautoconnect.R
import ca.etrak.wifiautoconnect.data.WiFiNetwork
import java.text.SimpleDateFormat
import java.util.*

class NetworkAdapter(
    private val onNetworkClick: (WiFiNetwork) -> Unit
) : ListAdapter<WiFiNetwork, NetworkAdapter.NetworkViewHolder>(NetworkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_network, parent, false)
        return NetworkViewHolder(view)
    }

    override fun onBindViewHolder(holder: NetworkViewHolder, position: Int) {
        holder.bind(getItem(position), onNetworkClick)
    }

    class NetworkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconWifi: ImageView = itemView.findViewById(R.id.iconWifi)
        private val iconLock: ImageView = itemView.findViewById(R.id.iconLock)
        private val textSsid: TextView = itemView.findViewById(R.id.textSsid)
        private val textDetails: TextView = itemView.findViewById(R.id.textDetails)
        private val textSignal: TextView = itemView.findViewById(R.id.textSignal)
        private val textLastSeen: TextView = itemView.findViewById(R.id.textLastSeen)
        private val iconConnected: ImageView = itemView.findViewById(R.id.iconConnected)

        private val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        fun bind(network: WiFiNetwork, onNetworkClick: (WiFiNetwork) -> Unit) {
            textSsid.text = network.ssid

            val details = "${network.securityType} | ${network.frequencyBand}"
            textDetails.text = details

            textSignal.text = "${network.signalStrength} dBm"
            textLastSeen.text = dateFormat.format(Date(network.lastSeenTimestamp))

            // Signal strength icon
            val signalIcon = when {
                network.signalStrength >= -50 -> R.drawable.ic_signal_4
                network.signalStrength >= -60 -> R.drawable.ic_signal_3
                network.signalStrength >= -70 -> R.drawable.ic_signal_2
                else -> R.drawable.ic_signal_1
            }
            iconWifi.setImageResource(signalIcon)

            // Lock icon visibility
            iconLock.visibility = if (network.isOpen) View.GONE else View.VISIBLE

            // Connected icon visibility
            iconConnected.visibility = if (network.connectionSuccessful) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onNetworkClick(network) }
        }
    }

    class NetworkDiffCallback : DiffUtil.ItemCallback<WiFiNetwork>() {
        override fun areItemsTheSame(oldItem: WiFiNetwork, newItem: WiFiNetwork): Boolean {
            return oldItem.bssid == newItem.bssid
        }

        override fun areContentsTheSame(oldItem: WiFiNetwork, newItem: WiFiNetwork): Boolean {
            return oldItem == newItem
        }
    }
}
