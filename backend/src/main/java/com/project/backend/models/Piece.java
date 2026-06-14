package com.project.backend.models;

import java.time.Duration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Entity
@Accessors(chain = true)
@NoArgsConstructor
public class Piece {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Size(max = 255, message = "Title must be under 255 characters")
  @NotNull(message = "A piece must have a name")
  @Column(name = "name", unique = true)
  private String title;

  @Size(max = 255, message = "Composer must be under 255 characters")
  @NotNull(message = "A piece must have a composer")
  @Column(name = "composer")
  private String composer;

  @NotNull(message = "A piece must have a price")
  @Column(name = "price")
  private Double price;

  @NotNull(message = "A piece must have a description")
  @Column(name = "description", length = 2000)
  private String description;

  // this is just a year, not a whole date
  @PastOrPresent(message = "The year composed must be in either the past or present")
  @NotNull(message = "A piece must have a date in which it was composed")
  @Column(name = "date_composed")
  private Integer yearComposed;

  @Column(name = "has_electronics")
  private boolean hasElectronics;

  @Column(name = "completed")
  private boolean completed;

  @NotNull(message = "We must know the number of players in a piece")
  @Column(name = "num_of_players")
  private Integer numOfPlayers;

  // this is a UIL way to claffisy pieces. Basically from 1-6
  @Max(value = 6, message = "difficulty must be from 1-6")
  @Min(value = 1, message = "difficulty must be from 1-6")
  @Column(name = "difficulty_grade")
  private Integer difficultyGrade;

  @NotNull(message = "A piece must have a length in time")
  @Column(name = "time_length")
  private Duration timeLength;

  // bare file name inside PIECE_FILES_DIR, never a path
  @Column(name = "file_name")
  private String fileName;

  public Piece(String title, String composer, Double price, String description, Integer yearComposed,
      boolean hasElectronics, boolean completed, Integer numOfPlayers, Integer difficultyGrade, Duration timeLength) {
    this.title = title;
    this.composer = composer;
    this.price = price;
    this.description = description;
    this.yearComposed = yearComposed;
    this.hasElectronics = hasElectronics;
    this.completed = completed;
    this.numOfPlayers = numOfPlayers;
    this.difficultyGrade = difficultyGrade;
    this.timeLength = timeLength;
  }
}
