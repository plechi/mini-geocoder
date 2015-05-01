/*
 Copyright 2015 Lukas Plechinger

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package at.plechinger.minigeocode.controller;

import at.plechinger.minigeocode.data.GeocodeResult;
import at.plechinger.minigeocode.data.ReverseGeocodeResult;
import at.plechinger.minigeocode.repository.GeocodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/geocode")
public class GeocodeController {

    @Autowired
    private GeocodeRepository geocodeRepository;

    @RequestMapping
    public GeocodeResult getGeocodingSingle(@RequestParam(value = "q", required = true) String query) {
        Slice<GeocodeResult> result= geocodeRepository.findSlice(query, new PageRequest(0, 1, Sort.Direction.ASC, "street", "housenumber"));

        if(result!=null && result.getContent()!=null && !result.getContent().isEmpty()){
            return result.getContent().get(0);
        }
        return null;
    }

    @RequestMapping("/all")
    public List<GeocodeResult> getGeocoding(@RequestParam(value = "q", required = true) String query, @PageableDefault(sort = {"street", "housenumber"}) Pageable pageable) {
        return geocodeRepository.findAll(query, pageable.getSort());
    }

    @RequestMapping(value = "/paged")
    public Page<GeocodeResult> getPagedGeocoding(@RequestParam(value = "q", required = true) String query, @PageableDefault(sort = {"street", "housenumber"}) Pageable pageable) {
        return geocodeRepository.findPage(query, pageable);
    }

    @RequestMapping(value = "/sliced")
    public Slice<GeocodeResult> getSlicedGeocoding(@RequestParam(value = "q", required = true) String query, @PageableDefault(sort = {"street", "housenumber"}) Pageable pageable) {
        return geocodeRepository.findSlice(query, pageable);
    }

}
