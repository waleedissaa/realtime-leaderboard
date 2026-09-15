package com.waleedissaa.leaderboard;

import org.springframework.boot.SpringApplication;

public class TestLeaderboardApplication {

	public static void main(String[] args) {
		SpringApplication.from(LeaderboardApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
