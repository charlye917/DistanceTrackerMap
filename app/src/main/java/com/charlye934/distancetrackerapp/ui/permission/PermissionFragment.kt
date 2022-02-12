package com.charlye934.distancetrackerapp.ui.permission

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.charlye934.distancetrackerapp.util.Permissions
import com.charlye934.distancetrackerapp.R
import com.charlye934.distancetrackerapp.databinding.FragmentPermissionBinding
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog

class PermissionFragment : Fragment(), EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentPermissionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding =  FragmentPermissionBinding.inflate(inflater, container, false)

        if(Permissions.hasLocationPermission(requireContext()))
            findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)

        binding.continueButton.setOnClickListener {
            checkLocation()
        }
        return binding.root
    }

    private fun checkLocation(){
        if(Permissions.hasLocationPermission(requireContext())){
            findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)
        }else{
            Permissions.requestLocationPermission(this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Log.d("__tag", "permiso denegado")
        if(EasyPermissions.somePermissionPermanentlyDenied(PermissionFragment(), perms)){
            SettingsDialog.Builder(requireActivity()).build().show()
        }else{
            Permissions.requestLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d("__tag", "permiso otorgado")
        findNavController().navigate(R.id.action_permissionFragment_to_mapsFragment)
    }


}