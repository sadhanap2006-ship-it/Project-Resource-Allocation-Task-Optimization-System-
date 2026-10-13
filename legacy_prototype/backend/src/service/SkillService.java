package service;

import dao.SkillDAO;
import dto.SkillDTO;
import model.Skill;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SkillService {

    private final SkillDAO skillDAO = new SkillDAO();

    public List<SkillDTO> getAllSkills() throws SQLException {
        List<Skill> list = skillDAO.findAll();
        List<SkillDTO> dtos = new ArrayList<>();
        for (Skill s : list) {
            dtos.add(new SkillDTO(s.getId(), s.getName(), s.getCategory()));
        }
        return dtos;
    }

    public SkillDTO createSkill(String name, String category) throws SQLException {
        Skill s = new Skill();
        s.setName(name);
        s.setCategory(category != null ? category : "General");
        int id = skillDAO.insert(s);
        return new SkillDTO(id, s.getName(), s.getCategory());
    }
}
