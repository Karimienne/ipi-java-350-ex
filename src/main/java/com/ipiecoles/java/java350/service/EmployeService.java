package com.ipiecoles.java.java350.service;

import com.ipiecoles.java.java350.exception.EmployeException;
import com.ipiecoles.java.java350.model.Employe;
import com.ipiecoles.java.java350.model.Entreprise;
import com.ipiecoles.java.java350.model.NiveauEtude;
import com.ipiecoles.java.java350.model.Poste;
import com.ipiecoles.java.java350.repository.EmployeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import java.time.LocalDate;

@Service
public class EmployeService {

    @Autowired
    private EmployeRepository employeRepository;

    /**
     * Méthode enregistrant un nouvel employé dans l'entreprise
     */
    public void embaucheEmploye(String nom, String prenom, Poste poste, NiveauEtude niveauEtude, Double tempsPartiel)
            throws EmployeException, EntityExistsException {

        String typeEmploye = poste.name().substring(0, 1);

        String lastMatricule = employeRepository.findLastMatricule();
        if (lastMatricule == null) {
            lastMatricule = Entreprise.MATRICULE_INITIAL;
        }

        Integer numeroMatricule = Integer.parseInt(lastMatricule) + 1;
        if (numeroMatricule >= 100000) {
            throw new EmployeException("Limite des 100000 matricules atteinte !");
        }

        String matricule = "00000" + numeroMatricule;
        matricule = typeEmploye + matricule.substring(matricule.length() - 5);

        if (employeRepository.findByMatricule(matricule) != null) {
            throw new EntityExistsException("L'employé de matricule " + matricule + " existe déjà en BDD");
        }

        Double salaire = Entreprise.COEFF_SALAIRE_ETUDES.get(niveauEtude) * Entreprise.SALAIRE_BASE;
        if (tempsPartiel != null) {
            salaire = salaire * tempsPartiel;
        }

        Employe employe = new Employe(
                nom,
                prenom,
                matricule,
                LocalDate.now(),
                salaire,
                Entreprise.PERFORMANCE_BASE,
                tempsPartiel
        );

        employeRepository.save(employe);
    }

    /**
     * Méthode calculant la performance d'un commercial
     */
    public void calculPerformanceCommercial(String matricule, Long caTraite, Long objectifCa) throws EmployeException {

        if (caTraite == null || caTraite < 0) {
            throw new EmployeException("Le chiffre d'affaire traité ne peut être négatif ou null !");
        }
        if (objectifCa == null || objectifCa < 0) {
            throw new EmployeException("L'objectif de chiffre d'affaire ne peut être négatif ou null !");
        }
        if (matricule == null || !matricule.startsWith("C")) {
            throw new EmployeException("Le matricule ne peut être null et doit commencer par un C !");
        }

        Employe employe = employeRepository.findByMatricule(matricule);
        if (employe == null) {
            throw new EmployeException("Le matricule " + matricule + " n'existe pas !");
        }

        double ratio = (double) caTraite / objectifCa;

        Integer performance = calculerPerformance(ratio, employe.getPerformance());

        Double performanceMoyenne = employeRepository.avgPerformanceWhereMatriculeStartsWith("C");
        if (performanceMoyenne != null && performance > performanceMoyenne) {
            performance++;
        }

        employe.setPerformance(performance);
        employeRepository.save(employe);
    }

    /**
     * Méthode privée pour réduire la complexité cognitive
     */
    private int calculerPerformance(double ratio, int performanceActuelle) {
        if (ratio < 0.8) {
            return Entreprise.PERFORMANCE_BASE;
        }
        if (ratio < 0.95) {
            return Math.max(Entreprise.PERFORMANCE_BASE, performanceActuelle - 2);
        }
        if (ratio <= 1.05) {
            return Math.max(Entreprise.PERFORMANCE_BASE, performanceActuelle);
        }
        if (ratio <= 1.2) {
            return performanceActuelle + 1;
        }
        return performanceActuelle + 4;
    }
}
