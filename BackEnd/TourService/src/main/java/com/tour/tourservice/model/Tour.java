package com.tour.tourservice.model;

import java.util.Date;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tour")
@Data
public class Tour {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private int id_tour;

	    @Column(name = "title")
	    private String title;

	    @Column(name = "description")
	    private String description;

	    @Column(name = "location")
	    private String location;

	    @Column(name = "duration")
	    private int duration;
	    
	    @Column(name = "price")
	    private double price;

	    @Column(name = "max_participants")
	    private int max_participants;
	    
	    @Column(name = "start_date")
	    private Date start_date;
	    
	    @Column(name = "end_date")
	    private Date end_date;
	    
	    @Column(name = "created_at")
	    private Date created_at;
}
