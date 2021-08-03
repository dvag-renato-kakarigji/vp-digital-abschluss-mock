package com.dvag.mock.rest

import com.dvag.mock.build.server.v1.api.BlueApi
import com.dvag.mock.build.server.v1.model.BlueDto
import io.swagger.annotations.ApiParam
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class BlueController : BlueApi{

    override fun getBlue(@ApiParam(value = "",required=true) @PathVariable("id") id: String): ResponseEntity<BlueDto>  {

        val blueDto = BlueDto().produktId(id)
        blueDto.put("ID", id)

        return ResponseEntity.ok(blueDto)
    }

}
