package com.company.projects.services;

import com.company.projects.annotations.IAmService;

@IAmService
public class Info implements IService {

    public Info() {
        announceInstantiation();
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
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

}
