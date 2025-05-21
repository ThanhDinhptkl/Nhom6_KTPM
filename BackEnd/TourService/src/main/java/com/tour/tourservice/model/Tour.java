package com.tour.tourservice.model;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tour")
public class Tour implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id_tour;

	private String title;

	private String description;

	private String location;

	private int duration;

	private double price;

	private int max_participants;

	private Date start_date;

	private Date end_date;

	private Date created_at;

	private String image;
}
