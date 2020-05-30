package com.company.projects.services;

import com.company.projects.annotations.IAmService;
import com.company.projects.annotations.INeedThis;

@IAmService
public class Office implements IService {

    private Director director;
    private Address address;
    private Info info;

    public Office() {
        announceInstantiation();
    }

    @INeedThis
    public Office(Director director, Address address, Info info) {
        this.director = director;
        this.address = address;
        this.info = info;
        announceInstantiation();
    }

    public Director getDirector() {
        return director;
    }

    public void setDirector(Director director) {
        this.director = director;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public void announceInstantiation() {
        System.out.printf("%s is instantiated.\n", this.toString());
    }

    @Override
    public void print() {
        System.out.println(this.toString());
    }

    @Override
    public void printDeep() {
        print();
        if (director != null) director.printDeep();
        if (address != null) address.printDeep();
        if (info != null) info.printDeep();
    }


    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
