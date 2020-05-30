package com.company.projects.services;

import com.company.projects.annotations.IAmService;
import com.company.projects.annotations.INeedThis;

@IAmService
public class Company implements IService {

    @INeedThis
    private Office office;
    private Director director;
    private Address address;
    private Info info;

    @INeedThis
    public Company(Director director, Address address) {
        this.director = director;
        this.address = address;
        announceInstantiation();
    }

    public Office getOffice() {
        return office;
    }

    public void setOffice(Office office) {
        this.office = office;
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

    @INeedThis
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
        if (office != null) office.printDeep();
        if (director != null) director.printDeep();
        if (address != null) address.printDeep();
        if (info != null) info.printDeep();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
