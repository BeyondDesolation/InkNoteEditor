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
import com.dusk73.musicxmltools.enums.Accidental
import com.dusk73.musicxmltools.enums.NoteType


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

        setClickListeners()
        binding.scoreView.updateMusicEditor(viewModel.musicEditor)
        binding.scoreView.update()

        return binding.root
    }

    private fun setClickListeners() {
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

        binding.addNote1.setOnClickListener {
            addNote(NoteType.WHOLE)
        }
        binding.addNote2.setOnClickListener {
            addNote(NoteType.HALF)
        }
        binding.addNote4.setOnClickListener {
            addNote(NoteType.QUARTER)
        }
        binding.addNote8.setOnClickListener {
            addNote(NoteType.EIGHTH)
        }
        binding.addNote16.setOnClickListener {
            addNote(NoteType._16TH)
        }
        binding.addNote32.setOnClickListener {
            addNote(NoteType._32EN)
        }
        binding.addNote64.setOnClickListener {
            addNote(NoteType._64TH)
        }
        binding.addRest.setOnClickListener {
            if(viewModel.setRest(binding.scoreView.touchInfo))
                binding.scoreView.update()
        }

        binding.deleteNote.setOnClickListener {
            if(viewModel.deleteNote(binding.scoreView.touchInfo))
                binding.scoreView.update()
        }

        binding.addSharp.setOnClickListener{
            changeAccidental(Accidental.SHARP)
        }
        binding.addFlat.setOnClickListener{
            changeAccidental(Accidental.FLAT)
        }
        binding.addNatural.setOnClickListener {
            changeAccidental(Accidental.NATURAL)
        }

        binding.addMeasure.setOnClickListener{
            if(viewModel.addMeasure(binding.scoreView.touchInfo))
                binding.scoreView.update()
        }
        binding.deleteMeasure.setOnClickListener{
            if(viewModel.deleteMeasure(binding.scoreView.touchInfo))
                binding.scoreView.update()
        }
        binding.addPart.setOnClickListener{
            if(viewModel.addPart(binding.scoreView.touchInfo))
                binding.scoreView.update()
        }
        binding.deletePart.setOnClickListener{
            if(viewModel.deletePart(binding.scoreView.touchInfo))
                binding.scoreView.update()
        }
    }

    private fun addNote(type: NoteType, rest: Boolean = false) {
        if(viewModel.addNote(binding.scoreView.touchInfo, type))
            binding.scoreView.update()
    }

    private fun changeAccidental(value: Accidental) {
        if(viewModel.changeAccidental(binding.scoreView.touchInfo, value))
            binding.scoreView.update()
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