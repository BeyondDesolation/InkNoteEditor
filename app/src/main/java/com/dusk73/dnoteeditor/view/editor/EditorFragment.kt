package com.dusk73.dnoteeditor.view.editor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.dusk73.dnoteeditor.databinding.FragmentEditorBinding


class EditorFragment : Fragment() {
    private val TAG = "EditorFragment"
    private lateinit var viewModel: EditorViewModel

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

    private var hasWritePermission = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this)[EditorViewModel::class.java]
        _binding = FragmentEditorBinding.inflate(inflater, container, false)

        binding.button.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
                getWritePermission()
            } else {
                hasWritePermission = true
            }
            if(hasWritePermission){
                viewModel.save()
            }
        }
        return binding.root
    }

    private fun getWritePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            hasWritePermission = true
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasWritePermission = if (isGranted) {
            Log.i(TAG, "PERMISSION GRANTED")
            true
        } else {
            Log.i(TAG, "PERMISSION NOT GRANTED")
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}