import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import android.provider.MediaStore
import android.content.ContentResolver
import com.example.practical_1.R

class RecordingAdapter(private val context: Context, private val recordings: MutableList<Uri>, private val contentResolver: ContentResolver, private val onDeleteAll: () -> Unit) :
    RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder>() {

    inner class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileName: TextView = itemView.findViewById(R.id.recordingFileName)
        val playButton: Button = itemView.findViewById(R.id.btnPlayRecording)
        val deleteButton: Button = itemView.findViewById(R.id.btnDeleteRecording)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recordingUri = recordings[position]
        val recordingNumber = recordings.size - position // Descending numbering
        holder.fileName.text = context.getString(R.string.recording_number, recordingNumber)

        holder.playButton.setOnClickListener {
            val mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(context, recordingUri)
                    prepare()
                    start()
                    Toast.makeText(context, "Playing Recording $recordingNumber", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error playing file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        holder.deleteButton.setOnClickListener {
            try {
                contentResolver.delete(recordingUri, null, null)
                recordings.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, recordings.size)
                Toast.makeText(context, "Deleted Recording $recordingNumber", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = recordings.size
}
