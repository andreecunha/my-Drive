# myDrive - Distributed File Storage System

**myDrive** is a distributed file storage system designed to facilitate file sharing among students and faculty members. The system implements a client-server architecture with failover mechanisms to ensure reliability and availability.

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Key Features](#key-features)
3. [System Architecture](#system-architecture)
4. [Installation Guide](#installation-guide)

---

## Project Overview

This project implements a file-sharing platform with the following objectives:
- Develop a multi-threaded client-server architecture for remote file storage.
- Enable secure communication between clients and servers using TCP sockets.
- Integrate failover mechanisms using UDP to ensure application availability in case of server failure.
- Provide administrative functionalities for user management and system monitoring.

---

## Key Features

1. **Client Functionality:**
   - User authentication with username and password.
   - List and navigate directories on both client and server sides.
   - Upload and download files between client and server.
   - Change user passwords securely.

2. **Admin Console:**
   - User registration and management.
   - Monitor system health, including failover mechanisms and data replication.
   - View storage usage per user and overall.

3. **Failover Mechanism:**
   - Detect server failures via UDP heartbeats.
   - Automatically switch to a backup server in case of primary server failure.

---

## System Architecture

The system consists of the following components:
1. **Primary Server:** Handles client requests and communicates with the secondary server for data replication.
2. **Secondary Server:** Monitors the primary server via UDP and takes over in case of failure.
3. **Clients:** Interface for users to interact with the system via TCP sockets.
4. **Admin Console:** Allows system administrators to manage users and monitor system health using RMI (for three-member teams).

---

## Installation Guide

1. Clone the repository:
   ```bash
   git clone https://github.com/andreecunha/my-Drive.git
