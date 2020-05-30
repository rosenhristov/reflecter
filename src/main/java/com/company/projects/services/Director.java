package com.company.projects.services;

import com.company.projects.annotations.IAmService;
import com.company.projects.annotations.INeedThis;

@IAmService
public class Director implements IService {

    private Address address;
    private Info info;

    public Director() {
        announceInstantiation();
    }

    public Address getAddress() {
        return address;
    }

    @INeedThis
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
        if (address != null) address.printDeep();
        if (info != null) info.printDeep();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
