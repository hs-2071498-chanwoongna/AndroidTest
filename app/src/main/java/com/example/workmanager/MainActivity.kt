package com.example.workmanager

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import java.util.concurrent.TimeUnit



class MyViewModel(context: Context) : ViewModel() {
    private val repository = MyRepository(context) // repository 객체 생성
    val repos = repository.repos
    // repository가 제공하는 repos를 repos 속성에 그대로 연결

    class Factory(val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel > create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MyViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MyViewModel(context) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}


class MyAdapter(val items:List<Repo>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>(){
    class MyViewHolder(v: View) : RecyclerView.ViewHolder(v){
        val repo = v.findViewById<TextView>(R.id.tvRepo)
        val owner = v.findViewById<TextView>(R.id.tvOwner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.repo.text = items[position].name
        holder.owner.text = items[position].owner.login
    }

    /*fun updateData(newItems: List<Repo>) {
        items = newItems
        notifyDataSetChanged()
    }*/
}

class MainActivity : AppCompatActivity() {
    private lateinit var myViewModel : MyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MyAdapter(emptyList())

        findViewById<Button>(R.id.startWorker).setOnClickListener {
            val username = findViewById<EditText>(R.id.editUsername).text.toString()
            startWorker(username)

        }
        findViewById<Button>(R.id.stopWorker).setOnClickListener { stopWorker() }

        myViewModel = ViewModelProvider(this, MyViewModel.Factory(this)).get(MyViewModel::class.java)
        myViewModel.repos.observe(this) { reposD ->
            val repos = reposD.map {
                Repo(it.name, Owner(it.owner),"")
            }
            recyclerView.adapter = MyAdapter(repos)
        }
    }

    private fun startWorker(username: String) {
        val constraints = Constraints.Builder().apply {
            setRequiredNetworkType(NetworkType.UNMETERED) // un-metered network such as WiFi
            setRequiresBatteryNotLow(true)
        }.build()

        val repeatingRequest = PeriodicWorkRequestBuilder<MyWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(workDataOf("username" to username))
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MyWorker.name,
            ExistingPeriodicWorkPolicy.UPDATE,
            repeatingRequest)

    }

    private fun stopWorker() {
        // to stop the MyWorker
        WorkManager.getInstance(this).cancelUniqueWork(MyWorker.name)
    }
}

