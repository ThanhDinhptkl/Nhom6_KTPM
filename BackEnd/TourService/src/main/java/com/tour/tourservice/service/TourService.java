package com.tour.tourservice.service;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tour.tourservice.dto.TourDTO;
import com.tour.tourservice.model.Tour;
import com.tour.tourservice.repository.TourRepository;

public interface TourService {
	void add(TourDTO tourDTO);

	void update(TourDTO tourDTO);

	void delete(int id);

	List<TourDTO> getAll();

	TourDTO getOne(int id);

}

@Transactional
@Service
class TourServiceImpl implements TourService {
	
	@Autowired
	TourRepository tourRepository;
	
	@Autowired
	ModelMapper modelMapper;
	
	@Override
	public void add(TourDTO tourDTO) {
		Tour tour = modelMapper.map(tourDTO, Tour.class);	
		tourRepository.save(tour);
		tourDTO.setId_tour(tour.getId_tour());
		// TODO Auto-generated method stub

	}

	@Override
	public void update(TourDTO tourDTO) {
		Tour tour = tourRepository.getById(tourDTO.getId_tour());
		if (tour != null) {
			modelMapper.typeMap(TourDTO.class, Tour.class)
			.addMappings(mapper -> mapper.skip(Tour::setCreated_at))
			.map(tourDTO, tour);
			tourRepository.save(tour);
		}
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(int id) {
		Tour tour = tourRepository.getById(id);
		if (tour != null) {
			tourRepository.delete(tour);
		}
		// TODO Auto-generated method stub

	}

	@Override
	public List<TourDTO> getAll() {
		List<TourDTO> tourDTOs = new ArrayList<>();
		tourRepository.findAll().forEach((tour) -> {
			tourDTOs.add(modelMapper.map(tour, TourDTO.class));
		});
		// TODO Auto-generated method stub
		return tourDTOs;
	}

	@Override
	public TourDTO getOne(int id) {
		Tour tour = tourRepository.getById(id);
		if (tour != null) {
            return modelMapper.map(tour, TourDTO.class);
		// TODO Auto-generated method stub
	}
		return null;
	}
}
