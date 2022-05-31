package com.example.perceptualhashalgorithmv2

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.perceptualhashalgorithmv2.databinding.ActivityMainBinding
import com.example.perceptualhashalgorithmv2.databinding.RecItemBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private val adapter = RecAdapter { callback ->
        b.tvCount.text = "Count : " + callback
    }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                viewModel.getImagePathListFromSystem()
            } else
                requestPermission()

        }

    private fun requestPermission() {
        permissionLauncher.launch(READ_EXTERNAL_STORAGE)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater).also { b ->
            setContentView(b.root)
            b.button.setOnClickListener {
                threshHold = b.et.text.toString().toInt()
                adapter.setList(viewModel.calculate(viewModel.getImagePathList().value!!))
            }
        }
        viewModel = ViewModelProvider(this)[MainViewModel::class.java].also {
            setupObservers(it)
        }
        permissionLauncher.launch(READ_EXTERNAL_STORAGE)
    }

    private fun setupObservers(mainViewModel: MainViewModel) {
        with(mainViewModel) {
            getImagePathList().observe(this@MainActivity) {
                val list = viewModel.calculate(it)
                b.recycler.adapter = adapter
                CoroutineScope(Main).launch {
                    adapter.setList(list)
                }
                b.tvSize.text = "Size : " + list.size

            }
        }
    }

    companion object {
        var threshHold = 95
    }

    inner class RecAdapter(val callback: (int: Int) -> Unit) :
        RecyclerView.Adapter<RecAdapter.RecHolder>() {
        var list = ArrayList<Group>()

        @JvmName("setList1")
        fun setList(list: ArrayList<Group>) {
            this.list = list
            notifyDataSetChanged()
        }

        inner class RecHolder(val b: RecItemBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RecHolder(
            RecItemBinding.inflate(
                LayoutInflater.from(parent.context)
            )
        )

        override fun onBindViewHolder(holder: RecHolder, position: Int) {
            val item = list[position]
            log("BINDVIEWHOLDER")
            with(holder.b) {
                Glide.with(holder.itemView.context).load(item.original.path).into(imageView)
                Glide.with(holder.itemView.context).load(item.list.first().path).into(imageView2)
                textView.text =
                    item.original.path.substring(item.original.path.length - 10, item.original.path.length)
                textView2.text =
                    item.list.first().path.substring(
                        item.list.first().path.length - 10,
                        item.list.first().path.length
                    )
            }
            callback.invoke(position)

        }

        override fun getItemCount() = list.size
    }

}